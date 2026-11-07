package ma.shopflow.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Le corps JSON d'une demande de commande et ses contraintes de validation. */
public record DemandeCommande(
        @NotBlank String clientId,
        @NotBlank @Email String email,
        @NotEmpty List<@NotNull @Valid Ligne> lignes,
        String codePromo) {

    public record Ligne(@NotBlank String reference, @Min(1) int quantite) {
    }
}
