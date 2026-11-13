package pe.edu.pucp.gtics.lab09.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jdk.jfr.events.CertificateId;

@Entity
@Table(name = "stf_puzzle_sv")

public class PuzzleSv {
    @Id
    @Column(name = "stf_game_board_structure")
    private String structure;

    @Column(name = "image")
    private String image;
    







}
