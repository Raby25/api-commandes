package payetonkawa.api_commande.model;

public enum StatutCommande {
    EN_ATTENTE, // Commande créée mais pas encore traitée
    EN_COURS, // Traitement ou préparation
    EXPEDIEE, // Envoyée au client mais pas encore réçu par le client
    LIVREE, // Client a reçu la commande
    ANNULEE // Commande annulée
}
