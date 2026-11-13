package pe.edu.pucp.gtics.lab09.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.gtics.lab09.entity.PuzzleSv;
import pe.edu.pucp.gtics.lab09.repository.PuzzleRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/puzzle")
public class PuzzleController {

    /*
     * Cada ficha se representa con un caracter y el espacio vacío con 'X'.
     * 3x3 -> "12345678X" (igual al formato pedido en el enunciado).
     * 4x4 y 5x5 continúan con letras: 1..9, A, B, C, ... (sin usar la X).
     */
    private static final String FICHAS = "123456789ABCDEFGHIJKLMNOP";
    private static final char VACIO = 'X';
    private static final int MIN_N = 3;
    private static final int MAX_N = 5;
    private static final Pattern FORMATO_DIMENSION = Pattern.compile("^([3-5])x\\1$");

    /*
     * Única secuencia de 8 pasos para desordenar el tablero (partiendo de la imagen ordenada).
     * Indica hacia dónde se mueve el espacio vacío: L=izquierda, U=arriba, R=derecha, D=abajo.
     * En 3x3: 12345678X -> 1234567X8 -> 123456X78 -> 123X56478 -> 1235X6478
     *                   -> 1X3526478 -> 13X526478 -> 13652X478 -> 13652847X
     */
    private static final String SECUENCIA_DESORDEN = "LLURURDD";

    private final PuzzleRepository puzzleRepository;

    public PuzzleController(PuzzleRepository puzzleRepository) {
        this.puzzleRepository = puzzleRepository;
    }

    /* ===================== GET: obtener estado del juego ===================== */
    @GetMapping
    public ResponseEntity<HashMap<String, Object>> obtenerJuego() {
        HashMap<String, Object> respuesta = new HashMap<>();
        Optional<PuzzleSv> opt = obtenerRegistro();
        if (opt.isEmpty()) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "No se encontró el juego.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
        }

        PuzzleSv puzzle = opt.get();
        String imagen = puzzle.getImage() == null ? "" : puzzle.getImage();
        String estructura = puzzle.getStructure();

        respuesta.put("estado", "ok");
        respuesta.put("estructura", estructura);
        respuesta.put("dimension", dimensionDe(estructura));
        respuesta.put("imagen", imagen);
        // Si hay una imagen guardada, hay un juego en curso que se debe retomar
        respuesta.put("enJuego", !imagen.isBlank());
        return ResponseEntity.ok(respuesta);
    }

    /* ===================== POST: iniciar juego ===================== */
    @PostMapping
    public ResponseEntity<HashMap<String, Object>> iniciarJuego(@RequestBody HashMap<String, String> body) {
        HashMap<String, Object> respuesta = new HashMap<>();
        String imagen = body.get("imagen");
        String dimension = body.get("dimension");

        // Solo se aceptan imágenes JPG
        if (imagen == null || !(imagen.startsWith("data:image/jpeg;base64,") || imagen.startsWith("data:image/jpg;base64,"))) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "Debe enviar una imagen válida en formato JPG.");
            return ResponseEntity.badRequest().body(respuesta);
        }

        // Dimensión obligatoria con formato NxN (3x3, 4x4, 5x5)
        if (dimension == null || dimension.isBlank()) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "Debe ingresar las dimensiones del tablero.");
            return ResponseEntity.badRequest().body(respuesta);
        }
        Matcher m = FORMATO_DIMENSION.matcher(dimension.trim().toLowerCase());
        if (!m.matches()) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "Formato inválido. Use NxN, por ejemplo 3x3, 4x4 o 5x5.");
            return ResponseEntity.badRequest().body(respuesta);
        }
        int n = Integer.parseInt(m.group(1));

        String estructuraInicial = desordenar(n);
        int filas = puzzleRepository.actualizarImagenYEstructura(imagen, estructuraInicial);
        if (filas == 0) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "No hay registro del juego en la base de datos.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
        }

        respuesta.put("estado", "creado");
        respuesta.put("estructura", estructuraInicial);
        respuesta.put("dimension", n);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /* ===================== PUT: mover una ficha ===================== */
    @PutMapping("/movimiento")
    public ResponseEntity<HashMap<String, Object>> mover(@RequestBody HashMap<String, Integer> body) {
        HashMap<String, Object> respuesta = new HashMap<>();
        Integer posicion = body.get("posicion");

        Optional<PuzzleSv> opt = obtenerRegistro();
        if (opt.isEmpty() || opt.get().getImage() == null || opt.get().getImage().isBlank()) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "No hay un juego en curso.");
            return ResponseEntity.badRequest().body(respuesta);
        }

        String estructura = opt.get().getStructure();
        int n = dimensionDe(estructura);
        if (posicion == null || posicion < 0 || posicion >= n * n) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "Posición no válida.");
            return ResponseEntity.badRequest().body(respuesta);
        }

        // Solo se puede mover una ficha contigua (arriba/abajo/izq/der) a la casilla vacía
        int vacio = estructura.indexOf(VACIO);
        int distancia = Math.abs(posicion / n - vacio / n) + Math.abs(posicion % n - vacio % n);
        if (distancia != 1) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "Movimiento no válido: la ficha no es contigua al espacio vacío.");
            return ResponseEntity.badRequest().body(respuesta);
        }

        char[] fichas = estructura.toCharArray();
        fichas[vacio] = fichas[posicion];
        fichas[posicion] = VACIO;
        String nueva = new String(fichas);

        respuesta.put("estado", "ok");
        respuesta.put("estructura", nueva);
        respuesta.put("dimension", n);

        if (nueva.equals(estructuraOrdenada(n))) {
            // Juego terminado: se reinicia el juego en la BD (sin imagen = nuevo juego)
            puzzleRepository.actualizarImagenYEstructura("", desordenar(MIN_N));
            respuesta.put("terminado", true);
            respuesta.put("mensaje", "Juego terminado !");
        } else {
            puzzleRepository.actualizarEstructura(nueva);
            respuesta.put("terminado", false);
        }
        return ResponseEntity.ok(respuesta);
    }

    /* ===================== Manejo de JSON mal formado ===================== */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<HashMap<String, Object>> gestionarExcepcion(HttpServletRequest request) {
        HashMap<String, Object> respuesta = new HashMap<>();
        respuesta.put("estado", "error");
        switch (request.getMethod()) {
            case "POST" -> respuesta.put("mensaje", "Debe enviar una imagen y dimensión válidas.");
            case "PUT" -> respuesta.put("mensaje", "Debe enviar una posición válida.");
            default -> respuesta.put("mensaje", "Solicitud no válida.");
        }
        return ResponseEntity.badRequest().body(respuesta);
    }

    /* ===================== Métodos auxiliares ===================== */

    private Optional<PuzzleSv> obtenerRegistro() {
        List<PuzzleSv> lista = puzzleRepository.findAll();
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    private int dimensionDe(String estructura) {
        int n = (int) Math.round(Math.sqrt(estructura.length()));
        return (n < MIN_N || n > MAX_N || n * n != estructura.length()) ? MIN_N : n;
    }

    /** Tablero ordenado: "12345678X" para 3x3. */
    private String estructuraOrdenada(int n) {
        return FICHAS.substring(0, n * n - 1) + VACIO;
    }

    /** Aplica la secuencia fija de 8 pasos sobre el tablero ordenado. */
    private String desordenar(int n) {
        char[] tablero = estructuraOrdenada(n).toCharArray();
        int vacio = n * n - 1;
        for (char paso : SECUENCIA_DESORDEN.toCharArray()) {
            int destino = switch (paso) {
                case 'L' -> vacio - 1;
                case 'R' -> vacio + 1;
                case 'U' -> vacio - n;
                default -> vacio + n; // 'D'
            };
            tablero[vacio] = tablero[destino];
            tablero[destino] = VACIO;
            vacio = destino;
        }
        return new String(tablero);
    }
}
