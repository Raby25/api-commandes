package payetonkawa.api_commande.dto;

import java.util.Date;

import lombok.Data;

@Data
public class ProductDto {
    private Long id;
    private String libelle;
    private String description;
    private float price;
    private int stock;
    private Date createdAt;

}
