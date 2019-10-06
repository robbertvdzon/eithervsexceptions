package com.vdzon.monads.java;

import java.util.Optional;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
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

  @RequestMapping("/brief1")
  private ResponseEntity<String> briefController() {
    try {
      String result = sendBrief(0, "Bief inhoud").getResult();
      return ResponseEntity.ok(result);
    } catch (KlantException | AdresException | BriefException | SendException ex) {
      return ResponseEntity.status(500).body(ex.getMessage());
    }
  }

  private VerstuurResult sendBrief(int id, String body) throws KlantException, AdresException, BriefException, SendException {
    // haal klant op, throw Exceptie als niet gevonden
    final Optional<Klant> maybeKlant = zoekKlant(id);
    if (maybeKlant.isEmpty()) { throw new KlantException("Klant niet gevonden"); }
    final Klant klant = maybeKlant.get();

    // zoek of deze klant een zakelijke klant is
    final boolean isZakelijkeKlant = isZakelijkeKlant(klant);

    // zoek adres (afhankelijk van zakelijk of niet), als geen adres gevonden, throw een Exceptie
    final Optional<Adres> mayBeAdres = isZakelijkeKlant ? findWerkAdres(klant) : Optional.of(findPriveAdres(klant));
    if (mayBeAdres.isEmpty()) { throw new AdresException("Adres niet gevonden"); }
    Adres adres = mayBeAdres.get();

    // Genereer een brief (deze functie gooit zelf een exceptie als er iets misgaat)
    Brief brief = genereerBrief(adres, klant, body);

    // verstuur de brief (deze functie gooit zelf een exceptie als er iets misgaat)
    return verstuurBrief(brief);
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
