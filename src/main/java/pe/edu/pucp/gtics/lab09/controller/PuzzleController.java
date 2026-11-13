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
            respuesta.put("mensaje", "Estructura inválida para iniciar el juego.");
            return ResponseEntity.badRequest().body(respuesta);
        }

        int filasAfectadas = puzzleRepository.actualizarEstructura(ESTRUCTURA_INICAL);
        if (filasAfectadas > 0) {
            respuesta.put("estado", "ok");
            respuesta.put("mensaje", "Juego iniciado correctamente.");
            return ResponseEntity.ok(respuesta);
        } else {
            respuesta.put("estado", "error");
            respuesta.put("mensaje", "No se pudo iniciar el juego.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
    }


}
