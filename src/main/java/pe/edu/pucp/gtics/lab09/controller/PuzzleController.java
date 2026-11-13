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
import java.util.Map;
import java.util.List;


@RestController
@RequestMapping("/api/puzzle")
public class PuzzleController {
    private static final String ESTRUCTURA_INICAL = "13652847X";
    private static final String ESTRUCTURA_META = "12345678X";

    private final PuzzleRepository puzzleRepository;

    public PuzzleController(PuzzleRepository puzzleRepository) {
        this.puzzleRepository = puzzleRepository;
    }

    @GetMapping
    public ResponseEntity<HashMap<String, Object>> obtenerJuego() {
        HashMap<String, Object> respuesta = new HashMap<>();
        List<PuzzleSv> puzzleList = puzzleRepository.findAll();
        if (lista.isEmpty()) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "No se encontró el juego.");
            return ResponseEntity.badRequest().body(respuesta);
        }

        PuzzleSv puzzle = lista.get(0);
        respuesta.put("estado", "ok");
        respuesta.put("estructura", puzzle.getStructure());
        respuesta.put("imagen", puzzle.getImage());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping
    public ResponseEntity<HashMap<String, Object>> iniciarJuego(@RequestBody HashMap<String, String> body) {
        HashMap<String, Object> respuesta = new HashMap<>();
        String imagen = body.get("imagen");
        if (imagen == null || !imagen.startsWith("data:image"))) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "Enviar una imgen valida");
            return ResponseEntity.badRequest().body(respuesta);
        }

        int filas = puzzleRepository.actualizarImagenYEstructura(imagen, ESTRUCTURA_INICAL);
        if (filas == 0) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "No hay registro del juego en la base de datos.");
            return ResponseEntity.badRequest().body(respuesta);
        }
        respuesta.put("estado", "creado");
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PutMapping("/movimiento")
    public ResponseEntity<HashMap<String, Object>> mover(@RequestBody HashMap<String, Integer> body) {
        HashMap<String, Object> respuesta = new HashMap<>();

        Integer posicion = body.get("posicion");
        List<PuzzleSv> lista = puzzleRepository.findAll();

        if (posicion == null || posicion < 0 || posicion > 8 || lista.isEmpty()) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "No valido");
            return ResponseEntity.badRequest().body(respuesta);
        }

        String estructura = lista.get(0).getStructure();
        int vacio = estructura.indexOf("X");
        int distancia = Math.abs(posicion / 3 - vacio / 3) + Math.abs(posicion % 3 - vacio % 3);
        if (distancia != 1) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "Movimiento no valido");
            return ResponseEntity.badRequest().body(respuesta);
        }

        char[] fichas = estructura.toCharArray();
        fichas[vacio] = fichas[posicion];
        fichas[posicion] = 'X';
        String nueva = new String(fichas);

        respuesta.put("estado", "ok");
        respuesta.put("estructura", nueva);
        if (nueva.equals(ESTRUCTURA_META)) {
            puzzleRepository.actualizarImagenYEstructura("", ESTRUCTURA_INICAL);
            respuesta.put("terminado", true);
        } else {
            puzzleRepository.actualizarEstructura(nueva);
            respuesta.put("terminado", false);
        }
        return ResponseEntity.ok(respuesta);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<HashMap<String, Object>> gestionarExcepcion(HttpServletRequest request) {
        HashMap<String, Object> respuesta = new HashMap<>();
        if (request.getMethod().equals("POST")) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "Enviar una imgen valida");
        } else if (request.getMethod().equals("PUT")) {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "No valido");
        } else {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "Solicitud no valida");
        }
        return ResponseEntity.badRequest().body(respuesta);
    }


}
