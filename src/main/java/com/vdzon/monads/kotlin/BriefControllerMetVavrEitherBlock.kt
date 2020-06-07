package com.vdzon.monads.kotlin

import io.vavr.control.Either
import io.vavr.control.Option
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@RestController
class BriefControllerMetVavrEitherBlock {

/*
  Haal klant op en zoek het adres. Stel daarna een brief samen en verstuur deze.

  TODO: maak een eigen Error class met daarin of het een userfout of systeem fout is
        bij systeem fout: 500 terug, bij userfout: 400 terug


  Stappen:
  1: haal klant op (kan mis gaan, dus foutmelding afvangen)
  2: roep functie aan die aangeeft of het een zakelijke klant is of niet
  3: haal huisadres (bestaat altijd) of werkadres (kan niet bestaan, dan fout teruggeven) op
  4: Genereer brief (kan mis gaan, dus foutmelding afvangen)
  5: Verstuur brief (kan mis gaan, dus foutmelding afvangen)

  Als er iets mis gaat moet het systeem een error 500 teruggeven met de goede foutmelding.
  Als er een userfout is (klant niet gevonden): dan een 400 teruggeven.
  Als het goed gaat geeft het systeem een 200 code terug met het resultaat van het versturen van de brief

  Opgelost met de vavr library in kotlin waarbij alle fouten via een Either doorgegeven worden.
  voordeel:
      - het is veel duidelijker welke fouten er in het systeek kunnen ontstaan
      - De "sendbrief" is zeer goed te lezen
  nadeel:
      - Het is even wennen om het Either type te gebruiken
      - wat is het verschil tussen en try/catch op alle fouten met runtime excepties? Dat je systeem fouten nog
        steeds met exceptie doe en dus een 500 krijg, maar user fouten kun je anders afhandelen.
        Is een system down dan een user-fout of een systeem fout? een systeem fout : 500 teruggegevn

  interne systeem fouten:
    hiervoor worden nog steeds excepties gebruikt. We catchen nergens een exceptie, dit wordt dus een 500 voor de gebruiker

  extern systeem fouten:
    dit gaat hier niet goed! we geven altijd een 400 terug

  request fouten:
    dit gaat goed: we geven een 400 terug
 */

    @PostMapping("/brief3/{klantid}\"")
    private fun briefController(@PathVariable id: Int, @RequestBody body: String): ResponseEntity<String> {
        return eitherBlock<String, VerstuurResult> {
            val klant = zoekKlant(id).bind()
            val zakelijkeKlant = isZakelijkeKlant(klant)
            val adres = findAdres(zakelijkeKlant, klant).bind()
            val brief = genereerBrief(adres, klant, body).bind()
            val verstuurResult = verstuurBrief(brief).bind()
            verstuurResult
        }.fold(
                { err -> ResponseEntity.status(400).body(err) },
                { result -> ResponseEntity.ok(result.result) }
        )
    }


    /*
    Onderstaande functies bevatten dummy implementaties, in dit voorbeeld gaat het om de signature
     */
    private fun findAdres(zakelijkeKlant: Boolean, klant: Klant) =
            if (zakelijkeKlant) findWerkAdres(klant).toEither { "Adres niet gevonden" }
            else Either.right(findPriveAdres(klant))
    private fun zoekKlant(id: Int): Either<String, Klant> = if (id == 0) Either.left("Niet gevonden") else Either.right(Klant(id, if (id == 1) "Robbert" else "Jan"))
    private fun isZakelijkeKlant(klant: Klant): Boolean = klant.naam.equals("Robbert")
    private fun findPriveAdres(klant: Klant): Adres = Adres("privestraat", 1, "Adam")
    private fun findWerkAdres(klant: Klant): Option<Adres> = Option.of(Adres("werkstraat", 2, "Rdam"))
    private fun genereerBrief(adres: Adres, klant: Klant, body: String): Either<String, Brief> = Either.right(Brief(body, adres, klant))
    private fun verstuurBrief(brief: Brief): Either<String, VerstuurResult> = Either.right(VerstuurResult("ok"))

    data class Klant(val id: Int, val naam: String)
    data class Adres(val straat: String, val huisnummer: Int, val woonplaats: String)
    data class Brief(val body: String, val adres: Adres, val klant: Klant)
    data class VerstuurResult(val result: String)


    fun <A, B> eitherBlock(c: suspend () -> B): Either<A, B> =
            try {
                Either.right(runBlocking { c.invoke() })
            } catch (e: EitherBlockException) {
                Either.left(e.left as A)
            }

    suspend fun <A, B> Either<A, B>.bind(): B = this.getOrElseThrow { _ -> EitherBlockException(this.left as Any) }  // suspend, om te forceren dat hij alleen maar binnen de eitherBlock draait

    class EitherBlockException(val left: Any) : Exception()

}
