package payetonkawa.api_commande.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ProductDto {


    private Long id;
    @JsonProperty("name")
    private String name;
    private String description;
    private float price;
    private int stock;
    private Date createdAt;

    
    public String getLibelle() {
        return this.name;
    }

    public void setLibelle(String libelle) {
        this.name = libelle;
    }
}
