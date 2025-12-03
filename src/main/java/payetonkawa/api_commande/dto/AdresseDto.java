package payetonkawa.api_commande.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdresseDto implements Serializable {

    private int numeroRue;
    private String rue;
    private String ville;
    private String codePostal;
    private String pays;
}
