package com.vdzon.monads.java;

import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BriefControllerMetVavr {

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

  Opgelost met vavr waarbij alle fouten via een Either doorgegeven worden.
  voordeel:
      - het is veel duidelijker welke fouten er in het systeek kunnen ontstaan
      - De "sendbrief" is beter te lezen dan de "checked exception" variant van de code
  nadeel:
      - Het is even wennen om het Either type te gebruiken
      - De "sendbrief" is minder duidelijk dan de "unchecked exception" variant van de code
 */

  @RequestMapping("/brief3")
  private ResponseEntity<String> briefController() {
    return sendBrief(0, "Bief inhoud")
        .fold(
            err -> ResponseEntity.status(500).body(err),
            result -> ResponseEntity.ok(result.getResult())
        );
  }

  private Either<String, VerstuurResult> sendBrief(int id, String body) {
    return zoekKlant(id).flatMap(klant -> {
      boolean isZakelijkeKlant = isZakelijkeKlant(klant);
      Option<Adres> mayBeAdres = isZakelijkeKlant ? findWerkAdres(klant) : Option.of(findPriveAdres(klant));
      return mayBeAdres.toEither("")
          .flatMap(adres -> genereerBrief(adres, klant, body)
          .flatMap(brief -> verstuurBrief(brief))
          );
    });
  }

  private Either<String, Klant> zoekKlant(int id) {
    if (id == 0) { return Either.left("Niet gevonden"); } else { return Either.right(new Klant(id, "Robbert")); }
  }

  private boolean isZakelijkeKlant(Klant klant) {
    return klant.getNaam().equals("Robbert");
  }

  private Adres findPriveAdres(Klant klant) {
    return new Adres("privestraat", 1, "Adam");
  }

  private Option<Adres> findWerkAdres(Klant klant) {
    return Option.of(new Adres("werkstraat", 2, "Rdam"));
  }

  private Either<String, Brief> genereerBrief(Adres adres, Klant klant, String body) {
    return Either.right(new Brief(body, adres, klant));
  }

  private Either<String, VerstuurResult> verstuurBrief(Brief brief) {
    return Either.right(new VerstuurResult("ok"));
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
