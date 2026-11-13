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

    private String getStructure() {
        return structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
    }
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
    
}
