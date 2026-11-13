// ===================== Stf-Puzzle Simplified Version =====================
// Todo el estado del juego vive en la BD y se maneja por Web Services REST (/api/puzzle)

const API = "api/puzzle";
const FICHAS = "123456789ABCDEFGHIJKLMNOP"; // mismo orden que en el backend
const VACIO = "X";
const TAM_TABLERO = 330;                    // px (igual que --tam en el CSS)
const FORMATO_DIMENSION = /^([3-5])x\1$/i;  // 3x3, 4x4, 5x5

let imagenActual = null;   // data URL de la imagen del juego
let estructura = "";       // ej. "13652847X"
let n = 3;                 // dimensión del tablero
let jugando = false;

$(function () {
    // Color del tablero (se recuerda en el navegador)
    try {
        const color = localStorage.getItem("colorTablero");
        if (color) $("#colorTablero").val(color);
    } catch (e) { /* sin almacenamiento */ }

    $("#colorTablero").on("input", function () {
        try { localStorage.setItem("colorTablero", $(this).val()); } catch (e) { }
        pintarTablero();
    });

    $("#btnSeleccionar").on("click", () => $("#inputImagen").trigger("click"));
    $("#inputImagen").on("change", cargarImagen);
    $("#dimension").on("input", validarDimension);
    $("#btnIniciar").on("click", iniciarJuego);
    $("#tablero").on("click", ".casilla", function () {
        moverFicha(parseInt($(this).data("pos")));
    });

    obtenerJuego();
});

// ===================== GET: retomar juego si existe =====================
function obtenerJuego() {
    $.ajax({
        method: "GET",
        url: API,
        dataType: "json"
    }).done(function (data) {
        if (data.enJuego) {
            imagenActual = data.imagen;
            estructura = data.estructura;
            n = data.dimension;
            mostrarJuego();
        } else {
            mostrarInicio();
        }
    }).fail(function (xhr) {
        mostrarError(xhr, "No se pudo obtener el juego.");
        mostrarInicio();
    });
}

// ===================== Selección de imagen (solo JPG) =====================
function cargarImagen() {
    const archivo = this.files[0];
    $("#errorImagen").addClass("d-none");
    if (!archivo) return;

    const nombre = archivo.name.toLowerCase();
    if (!(nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")) || archivo.type !== "image/jpeg") {
        $("#errorImagen").text("Solo se permiten imágenes en formato JPG.").removeClass("d-none");
        $(this).val("");
        return;
    }

    // Se muestra la nueva imagen sin recargar la página
    const lector = new FileReader();
    lector.onload = e => $("#imgPreview").attr("src", e.target.result);
    lector.readAsDataURL(archivo);
}

// ===================== Validación de dimensiones =====================
function validarDimension() {
    const valor = $("#dimension").val().trim();
    const $error = $("#errorDimension");

    if (valor === "") {
        $error.text("Debe ingresar las dimensiones del tablero (ej. 3x3).").removeClass("d-none");
        $("#btnIniciar").addClass("d-none");
        return false;
    }
    if (!FORMATO_DIMENSION.test(valor)) {
        $error.text("Formato inválido. Use NxN: 3x3, 4x4 o 5x5.").removeClass("d-none");
        $("#btnIniciar").addClass("d-none");
        return false;
    }
    $error.addClass("d-none");
    $("#btnIniciar").removeClass("d-none");
    return true;
}

// ===================== POST: iniciar juego =====================
function iniciarJuego() {
    if (!validarDimension()) return;

    // Recorta la imagen a un cuadrado y la convierte a JPG (Base64) para guardarla en la BD
    recortarImagen($("#imgPreview")[0], function (imagenJpg) {
        $.ajax({
            method: "POST",
            url: API,
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({
                imagen: imagenJpg,
                dimension: $("#dimension").val().trim().toLowerCase()
            })
        }).done(function (data) {
            imagenActual = imagenJpg;
            estructura = data.estructura;
            n = data.dimension;
            mostrarJuego();
        }).fail(xhr => mostrarError(xhr, "No se pudo iniciar el juego."));
    });
}

function recortarImagen(img, callback) {
    const lado = Math.min(img.naturalWidth, img.naturalHeight);
    const canvas = document.createElement("canvas");
    canvas.width = canvas.height = 600;
    const ctx = canvas.getContext("2d");
    ctx.drawImage(img,
        (img.naturalWidth - lado) / 2, (img.naturalHeight - lado) / 2, lado, lado,
        0, 0, 600, 600);
    callback(canvas.toDataURL("image/jpeg", 0.85));
}

// ===================== PUT: mover ficha =====================
function moverFicha(posicion) {
    if (!jugando || !esMovible(posicion)) return; // solo fichas contiguas al vacío

    $.ajax({
        method: "PUT",
        url: API + "/movimiento",
        contentType: "application/json",
        dataType: "json",
        data: JSON.stringify({ posicion: posicion })
    }).done(function (data) {
        estructura = data.estructura;
        pintarTablero();
        if (data.terminado) terminarJuego();
    }).fail(xhr => mostrarError(xhr, "Movimiento no válido."));
}

function esMovible(pos) {
    const vacio = estructura.indexOf(VACIO);
    return Math.abs(Math.floor(pos / n) - Math.floor(vacio / n)) + Math.abs(pos % n - vacio % n) === 1;
}

function terminarJuego() {
    jugando = false;
    $("#tablero").addClass("terminado");
    $("#mensajeJuego").text("Juego terminado !");
    // En la BD el juego ya se reinició; en el frontend volvemos a la pantalla inicial
    setTimeout(function () {
        $("#imgPreview").attr("src", "img/default.jpg");
        $("#dimension").val("");
        $("#inputImagen").val("");
        $("#btnIniciar").addClass("d-none");
        mostrarInicio();
    }, 3000);
}

// ===================== Dibujo =====================
function mostrarInicio() {
    jugando = false;
    $("#pantallaJuego").addClass("d-none");
    $("#pantallaInicio").removeClass("d-none");
}

function mostrarJuego() {
    jugando = true;
    $("#errorGeneral").addClass("d-none");
    $("#mensajeJuego").text("");
    $("#tablero").removeClass("terminado");
    $("#pantallaInicio").addClass("d-none");
    $("#pantallaJuego").removeClass("d-none");
    pintarTablero();
}

function pintarTablero() {
    if (!estructura) return;
    const $tablero = $("#tablero").empty();
    const tamCasilla = (TAM_TABLERO - 4 - (n - 1) * 2) / n;
    $tablero.css({
        "grid-template-columns": `repeat(${n}, 1fr)`,
        "background-color": $("#colorTablero").val()
    });

    for (let pos = 0; pos < estructura.length; pos++) {
        const ficha = estructura[pos];
        const $c = $("<div>").addClass("casilla").attr("data-pos", pos);

        if (ficha === VACIO) {
            $c.addClass("vacia").css("background-color", $("#colorTablero").val());
        } else {
            // índice original de la ficha en la imagen ordenada
            const idx = FICHAS.indexOf(ficha);
            const fila = Math.floor(idx / n), col = idx % n;
            $c.css({
                "background-image": `url(${imagenActual})`,
                "background-size": `${tamCasilla * n}px ${tamCasilla * n}px`,
                "background-position": `-${col * tamCasilla}px -${fila * tamCasilla}px`
            });
            if (jugando && esMovible(pos)) $c.addClass("movible");
        }
        $tablero.append($c);
    }
}

function mostrarError(xhr, porDefecto) {
    const msg = (xhr.responseJSON && xhr.responseJSON.mensaje) || porDefecto;
    const $e = $("#errorGeneral").text(msg).removeClass("d-none");
    setTimeout(() => $e.addClass("d-none"), 3000);
}
