package com.vdzon.monads.java;

import java.util.Optional;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BriefControllerMetCheckedException {

/*
  Haal klant op en zoek het adres. Stel daarna een brief samen en verstuur deze.

  Stappen:
  1: haal klant op (kan mis gaan, dus foutmelding afvangen)
  2: roep functie aan die aangeeft of het een zakelijke klant is of niet
  3: haal huisadres (bestaat altijd) of werkadres (kan niet bestaan, dan fout teruggeven) op
  4: Genereer brief (kan mis gaan, dus foutmelding afvangen)
  5: Verstuur brief (kan mis gaan, dus foutmelding afvangen)

  Als er iets mis gaat moet het systeem een error 500 teruggeven met de goede foutmelding.
  Als het goed gaat geeft het systeem een 200 code terug met het resultaat van het versturen van de brief

  Opgelost met standaard java waarbij alle fouten via checked excepties doorgegeven worden.
  voordeel:
      - de code van de losse functies zijn makkelijk te begrijpen
  nadeel:
      - checked excepties zorgen er voor dat we geen stream en lambda's kunnen gebruiken
      - De "sendBrief" waarin de echte logica zit is slecht te lezen
      - De kans is groot dat er stacktraces in je logging komen voor niet-systeem fouten
 */

  @PostMapping("/brief1/{klantid}")
  private ResponseEntity<String> briefController(@PathVariable int id, @RequestBody String body) {
    try {
      final Klant klant = zoekKlant(id).orElseThrow(() -> new KlantException("Klant niet gevonden"));
      final boolean isZakelijkeKlant = isZakelijkeKlant(klant);
      Adres adres = findAdres(klant, isZakelijkeKlant).orElseThrow(() -> new AdresException("Adres niet gevonden"));
      Brief brief = genereerBrief(adres, klant, body);
      VerstuurResult verstuurResult = verstuurBrief(brief);
      String result = verstuurResult.getResult();
      return ResponseEntity.ok(result);
    } catch (KlantException | AdresException | BriefException | SendException ex) {
      return ResponseEntity.status(400).body(ex.getMessage());
    }
    // overige excepties worden automatisch omgezet naar een error 500
  }


  @NotNull
  private Optional<Adres> findAdres(Klant klant, boolean isZakelijkeKlant) {
    return isZakelijkeKlant ? findWerkAdres(klant) : Optional.of(findPriveAdres(klant));
  }

  private Optional<Klant> zoekKlant(int id) {
    if (id == 0) { return Optional.empty(); } else { return Optional.of(new Klant(id, "Robbert")); }
  }

  private boolean isZakelijkeKlant(Klant klant) {
    return klant.getNaam().equals("Robbert");
  }

  private Adres findPriveAdres(Klant klant) {
    return new Adres("privestraat", 1, "Adam");
  }

  private Optional<Adres> findWerkAdres(Klant klant) {
    return Optional.of(new Adres("werkstraat", 2, "Rdam"));
  }

  private Brief genereerBrief(Adres adres, Klant klant, String body) throws BriefException {
    return new Brief(body, adres, klant);
  }

  private VerstuurResult verstuurBrief(Brief brief) throws SendException {
    return new VerstuurResult("ok");
  }

  private static class KlantException extends Exception {

    public KlantException(String msg) {
      super(msg);
    }
  }

  private static class AdresException extends Exception {

    public AdresException(String msg) {
      super(msg);
    }
  }

  private static class BriefException extends Exception {

    public BriefException(String msg) {
      super(msg);
    }
  }

  private static class SendException extends Exception {

    public SendException(String msg) {
      super(msg);
    }
  }


  @Value
  private static class Klant {

    int id;
    String naam;
  }

  @Value
  private static class Adres {

    String straat;
    int huisnummer;
    String woonplaats;
  }

  @Value
  private static class Brief {

    String body;
    Adres adres;
    Klant klant;
  }

  @Value
  private static class VerstuurResult {

    String result;
  }

}
