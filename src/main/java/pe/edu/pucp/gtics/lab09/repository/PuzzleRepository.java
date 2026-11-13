package pe.edu.pucp.gtics.lab09.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.gtics.lab09.entity.PuzzleSv;

@Repository
public interface PuzzleRepository extends JpaRepository<PuzzleSv, String> {
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = """ UPDATE stf_puzzle_sv
                        SET stf_game_board_structure = :estructura """, nativeQuery = true)
    int actualizarEstructura(@Param("estructura") String estructura);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = """ UPDATE stf_puzzle_sv
                        SET image = :imagen, stf_game_board_structure = :estructura """, nativeQuery = true)
    int actualizarImagenYEstructura(@Param("imagen") String imagen, @Param("estructura") String estructura);

}