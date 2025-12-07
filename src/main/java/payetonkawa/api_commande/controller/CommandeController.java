package payetonkawa.api_commande.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import payetonkawa.api_commande.dto.CommandeDto;
import payetonkawa.api_commande.model.Commande;
import payetonkawa.api_commande.model.LigneCommande;
import payetonkawa.api_commande.services.CommandeService;

@RestController
@RequestMapping("/commandes")
@RequiredArgsConstructor
@Slf4j
public class CommandeController {

    private final CommandeService service;

    @PostMapping
    public ResponseEntity<CommandeDto> create(@Valid @RequestBody CommandeDto commandeDto) {

        log.info("Requête de création de commande reçue pour le client: {}", commandeDto.getIdClient());
        CommandeDto createdCommande = service.create(commandeDto);
        log.info("Commande {} créée avec succès via le contrôleur.", createdCommande.getId());
        return new ResponseEntity<>(createdCommande, HttpStatus.CREATED);
    }

    @GetMapping
    public List<CommandeDto> all() {
        return service.all();
    }

    @GetMapping("/{id}")
    public Commande get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public CommandeDto update(@PathVariable Long id, @RequestBody Commande commande) {
        return service.update(id, commande);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
