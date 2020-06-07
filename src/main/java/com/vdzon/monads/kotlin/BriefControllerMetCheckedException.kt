package com.vdzon.monads.kotlin

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*


@RestController
class BriefControllerMetCheckedException {
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

  interne systeem fouten:
    hiervoor worden nog steeds excepties gebruikt. We catchen nergens een exceptie, dit wordt dus een 500 voor de gebruiker

  extern systeem fouten:
    hiervoor komen ook excepties, maar die vangen we niet af en worden dus een 500
    deze worden dus op dezelfde manier afgehandeld als de interne systeem fouten

  request fouten:
    dit gaat goed: we geven een 400 terug

 */
    @PostMapping("/brief1/{klantid}")
    private fun briefController(@PathVariable klantid: Int, @RequestBody body: String): ResponseEntity<String?> {
        return try {
            val klant: Klant = zoekKlant(klantid).orElseThrow { KlantException("Klant niet gevonden") }
            val isZakelijkeKlant: Boolean = isZakelijkeKlant(klant)
            val adres: Adres = findAdres(klant, isZakelijkeKlant).orElseThrow { AdresException("Adres niet gevonden") }
            val brief: Brief = genereerBrief(adres, klant, body)
            val verstuurResult: VerstuurResult = verstuurBrief(brief)
            val result: String = verstuurResult.result
            ResponseEntity.ok(result)
        } catch (ex: KlantException) {
            ResponseEntity.status(400).body(ex.message)
        } catch (ex: AdresException) {
            ResponseEntity.status(400).body(ex.message)
        } catch (ex: BriefException) {
            ResponseEntity.status(400).body(ex.message)
        } catch (ex: SendException) {
            ResponseEntity.status(400).body(ex.message)
        }
    }

    private fun findAdres(klant: Klant, isZakelijkeKlant: Boolean): Optional<Adres> = if (isZakelijkeKlant) findWerkAdres(klant) else Optional.of(findPriveAdres(klant))
    private fun zoekKlant(id: Int): Optional<Klant> = if (id == 0) {Optional.empty()} else {Optional.of(Klant(id, "Robbert"))}
    private fun isZakelijkeKlant(klant: Klant) = klant.naam == "Robbert"
    private fun findPriveAdres(klant: Klant) = Adres("privestraat", 1, "Adam")
    private fun findWerkAdres(klant: Klant) = Optional.of(Adres("werkstraat", 2, "Rdam"))
    private fun genereerBrief(adres: Adres, klant: Klant, body: String) = Brief(body, adres, klant)
    private fun verstuurBrief(brief: Brief) = VerstuurResult("ok")

    private class KlantException(msg: String?) : Exception(msg)
    private class AdresException(msg: String?) : Exception(msg)
    private class BriefException(msg: String?) : Exception(msg)
    private class SendException(msg: String?) : Exception(msg)

    data class Klant(val id: Int, val naam: String)
    data class Adres(val straat: String, val huisnummer: Int, val woonplaats: String)
    data class Brief(val body: String, val adres: Adres, val klant: Klant)
    data class VerstuurResult(val result: String)
}
