package com.vdzon.monads.kotlin

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@RestController
class Test3 {

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

  Opgelost met de arrow library in kotlin waarbij alle fouten via een Either doorgegeven worden.
  voordeel:
      - het is veel duidelijker welke fouten er in het systeek kunnen ontstaan
      - De "sendbrief" is zeer goed te lezen
  nadeel:
      - Het is even wennen om het Either type te gebruiken
 */

    @PostMapping("/brief3/{klantid}\"")
    private fun briefController(@PathVariable id: Int, @RequestBody body: String): ResponseEntity<String> {
        return Either.fx<String, VerstuurResult> {
            val klant = zoekKlant(id).bind()
            val zakelijkeKlant = isZakelijkeKlant(klant)
            val adres = findAdres(zakelijkeKlant, klant).bind()
            val brief = genereerBrief(adres, klant, body).bind()
            val result = verstuurBrief(brief).bind()
            result
        }.fold(
                { err -> ResponseEntity.status(400).body(err) },
                { result -> ResponseEntity.ok(result.result) }
        )
    }


    private fun findAdres(zakelijkeKlant: Boolean, klant: Klant) =
            if (zakelijkeKlant) findWerkAdres(klant).toEither { "Adres niet gevonden" }
            else Either.right(findPriveAdres(klant))

    private fun zoekKlant(id: Int): Either<String, Klant> = if (id == 0) Either.left("Niet gevonden") else Either.right(Klant(id, if (id == 1) "Robbert" else "Jan"))
    private fun isZakelijkeKlant(klant: Klant): Boolean = klant.naam.equals("Robbert")
    private fun findPriveAdres(klant: Klant): Adres = Adres("privestraat", 1, "Adam")
    private fun findWerkAdres(klant: Klant): Option<Adres> = Option.just(Adres("werkstraat", 2, "Rdam"))
    private fun genereerBrief(adres: Adres, klant: Klant, body: String): Either<String, Brief> = Either.right(Brief(body, adres, klant))
    private fun verstuurBrief(brief: Brief): Either<String, VerstuurResult> = Either.right(VerstuurResult("ok"))

    data class Klant(val id: Int, val naam: String)
    data class Adres(val straat: String, val huisnummer: Int, val woonplaats: String)
    data class Brief(val body: String, val adres: Adres, val klant: Klant)
    data class VerstuurResult(val result: String)

}
