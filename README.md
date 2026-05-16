# Abfahrtstafel Plugin – Vollständige Dokumentation

## Inhaltsverzeichnis

1. Einführung
2. Schnellstart
3. Typische Einsatzszenarien
4. Voraussetzungen
5. Installation
6. Grundprinzip
7. Befehle
8. TrainLines.yml
9. SoundBoxes.yml
10. WarnMessages.yml
11. DisplayLayouts.yml
12. Placeholder
13. Bedingungen (`showWhen`)
14. Layout-Elemente
15. Unterschiede zwischen Table und List
16. Tabellen (`type: table`)
17. Listen (`type: list`)
18. Variants
19. Sortierung (`sortBy`)
20. Sounds
21. TrainCarts-Trigger
22. Runtime-State
23. Vollständige Praxisbeispiele
24. Tipps und Best Practices

---

# 1. Einführung

Das Abfahrtstafel-Plugin simuliert dynamische Bahn-Anzeigen in Minecraft.

Es unterstützt:

* Abfahrtsanzeigen pro Gleis
* Gesamtübersichten für ganze Bahnhöfe
* Ankunftsstatus („In Kürze“)
* Verspätungen
* Warnhinweise
* Bahnhofsdurchsagen
* Zug-interne Durchsagen
* Frei gestaltbare Layouts
* Eigene Schriftarten

Die Darstellung erfolgt auf Maps bzw. Displays.

---

# 2. Schnellstart

## Ziel

Wir möchten eine Anzeigetafel für `München Hbf`, Gleis `10`, die die nächsten Abfahrten des ICE60 anzeigt und bei Einfahrt automatisch „In Kürze“ einblendet.

## Schritt 1: Zuglinie definieren

```yaml
trainLines:
  - name: ICE60
    description: München nach Hamburg
    orderedRailGroups:
      - orderIndex: 1
        name: "10"
        parentStation: München Hbf
        departures: 08:00,09:00,10:00
        arrivalPlatformSound: timberrail:an_muc
        arrivalTrainSound: timberrail:ice60_muc

      - orderIndex: 2
        name: "5"
        parentStation: Nürnberg Hbf
        departures: 09:05,10:05,11:05

      - orderIndex: 3
        name: "final"
        parentStation: Hamburg Hbf
        departures: final
```

## Schritt 2: SoundBox erstellen

```text
/abfahrtstafel soundbox create 10 München Hbf
```

## Schritt 3: Layout verwenden

```yaml
- type: table
  width: fill
  maxRows: 10
  columns:
    - header: "Abfahrt"
      width: 80
      value: "{time}"

    - header: "Ziel"
      width: fill
      value: "{destination}"

    - header: "Gleis"
      width: 50
      value: "{track}"
```

## Schritt 4: Trigger-Schilder setzen

### Ankunft

```text
[train]
ankunft
München Hbf:10
```

### Abfahrt

```text
[train]
abfahrt
München Hbf:10
```

## Schritt 5: Konfiguration laden

```text
/abfahrtstafel reload
```

---

# 3. Typische Einsatzszenarien

## Bahnsteiganzeige

Zeigt nur die nächsten Abfahrten eines bestimmten Gleises.

## Bahnhofsübersicht

Zeigt die nächsten Abfahrten aller Gleise eines Bahnhofs.

## Metro-/S-Bahn-Anzeige

Mit kurzen Zeilen und großen Schriften.

## Informationsdisplay

Mit Warnhinweisen und Sondermeldungen.

---

# 4. Voraussetzungen

* Paper
* BKCommonLib
* Train_Carts
* Optional: Resourcepack für eigene Sounds

Getestet wurde das Plugin bisher mit:

* Train_Carts: `v1.21.11-v2` (Build `1686`)
* BKCommonLib: `v1.21.11-v1` (Build `1957`)
* Server: `Paper 1.21.11-127-bd74vf6` (Minecraft `1.21.11`)

---

# 5. Installation

1. `Abfahrtstafel.jar` in den Ordner `plugins/` kopieren
2. Server starten
3. Konfigurationsdateien werden erstellt:

   * `config.yml`
   * `TrainLines.yml`
   * `SoundBoxes.yml`
   * `WarnMessages.yml`
   * `DisplayLayouts.yml`
4. Dateien anpassen
5. `/abfahrtstafel reload`

---

# 6. Grundprinzip

## Zuglinien

In `TrainLines.yml` definierst du:

* Linienname
* Reihenfolge der Stationen
* Gleise
* Abfahrtszeiten
* Sounds

## Displays

In `DisplayLayouts.yml` definierst du:

* Positionen
* Farben
* Schriftarten
* Tabellen
* Listen
* Inhalte und Bedingungen

## SoundBoxes

In `SoundBoxes.yml` definierst du:

* Ort des Lautsprechers
* Radius
* Lautstärke

## Trigger

Train_Carts-Schilder lösen aus:

* Ankunft
* Abfahrt

---

# 7. Befehle

## Allgemein

```text
/abfahrtstafel reload
```

Lädt alle Konfigurationen neu.

```text
/abfahrtstafel help
```

Zeigt Hilfe.

---

## Debug

```text
/abfahrtstafel debug fonts
```

Zeigt verfügbare Schriftarten.

```text
/abfahrtstafel debug soundbox <Gleis> <Bahnhof>
```

Spielt Testton an SoundBoxen.

```text
/abfahrtstafel debug trigger <Gleis> <Bahnhof>
```

Simuliert Trigger.

```text
/abfahrtstafel debug clearstate
```

Löscht verarbeitete Zustände.

---

## SoundBox-Verwaltung

```text
/abfahrtstafel soundbox create <Gleis> <Bahnhof>
```

Erstellt SoundBox an Spielerposition.

```text
/abfahrtstafel soundbox remove <ID>
```

Löscht SoundBox.

```text
/abfahrtstafel soundbox list
```

Listet SoundBoxen auf.

---

# 8. TrainLines.yml

Definiert Linien, Stationen und Fahrpläne.

## Beispiel

```yaml
trainLines:
  - name: ICE60
    description: München nach Hamburg
    orderedRailGroups:
      - orderIndex: 1
        name: "10"
        parentStation: München Hbf
        departures: 08:00,09:00,10:00
        arrivalPlatformSound: timberrail:an_muc
        arrivalTrainSound: timberrail:ice60_muc

      - orderIndex: 2
        name: "5"
        parentStation: Nürnberg Hbf
        departures: 09:05,10:05,11:05

      - orderIndex: 3
        name: "final"
        parentStation: Hamburg Hbf
        departures: final
        arrivalPlatformSound: timberrail:an_ham
```

---

## Felder

### `name`

Linienname.

### `description`

Beschreibung.

### `orderedRailGroups`

Liste der Stationen.

### `orderIndex`

Reihenfolge der Station.

### `name`

Gleisnummer.

### `parentStation`

Bahnhofsname.

### `departures`

Kommagetrennte Zeiten.

Oder:

```yaml
departures: final
```

für Endbahnhof.

### `arrivalPlatformSound`

Sound an der Station.

### `arrivalTrainSound`

Sound im Zug.

---

# 9. SoundBoxes.yml

Definiert Lautsprecherpositionen.

## Beispiel

```yaml
soundBoxes:
  - id: 1
    station: München Hbf
    railGroup: "10"
    world: world
    x: 100.5
    y: 65.0
    z: 200.5
    radius: 25.0
    volume: 1.0
    pitch: 1.0
```

---

## Felder

### `radius`

Reichweite in Blöcken.

### `volume`

Lautstärke.

### `pitch`

Tonhöhe.

---

# 10. WarnMessages.yml

Definiert Warnhinweise, die auf Anzeigen dargestellt werden können.

## Beispiel

```yaml
warnings:
  - id: stoerung_1
    station: München Hbf
    railGroup: "10"
    line: ICE60
    message: "Zug fällt heute aus"
    active: true
```

## Felder

### `id`

Eindeutige Kennung der Warnung.

### `station`

Bahnhof, für den die Warnung gilt.

### `railGroup`

Optionales Gleis. Wenn gesetzt, gilt die Warnung nur für dieses Gleis.

### `line`

Optionale Linie. Wenn gesetzt, gilt die Warnung nur für diese Linie.

### `message`

Anzuzeigender Text.

### `active`

`true` oder `false`.

## Verwendung im Layout

```yaml
value: "{warnings}"
showWhen: has_warnings
```

Mehrere aktive Warnungen werden automatisch mit `***` getrennt.

---

# 11. DisplayLayouts.yml

Definiert das Erscheinungsbild.

## Grundstruktur

```yaml
layouts:
  - name: standard
    width: 512
    height: 384
    background: "#000000"
    elements:
      - type: text
        x: 10
        y: 20
        value: "{station}"
```

---

# 12. Placeholder

| Placeholder      | Bedeutung             |
| ---------------- | --------------------- |
| `{station}`      | Bahnhof               |
| `{railGroup}`    | Gleis                 |
| `{line}`         | Linie                 |
| `{time}`         | Sollzeit              |
| `{expected}`     | Erwartete Zeit        |
| `{delay}`        | Verspätung            |
| `{delayMinutes}` | Verspätung in Minuten |
| `{destination}`  | Ziel                  |
| `{via}`          | Zwischenhalte         |
| `{track}`        | Gleis                 |
| `{warnings}`     | Warnhinweise          |

---

# 13. Bedingungen (`showWhen`)

| Wert            | Bedeutung               |
| --------------- | ----------------------- |
| `has_departure` | Es gibt Abfahrten       |
| `no_departure`  | Keine Abfahrten         |
| `has_delay`     | Verspätung vorhanden    |
| `no_delay`      | Keine Verspätung        |
| `has_via`       | Zwischenhalte vorhanden |
| `no_via`        | Keine Zwischenhalte     |
| `has_warnings`  | Warnungen vorhanden     |
| `no_warnings`   | Keine Warnungen         |
| `on_arrival`    | Zug fährt ein           |
| `no_arrival`    | Kein Ankunftsstatus     |

Mehrere Bedingungen:

```yaml
showWhen: has_departure,no_delay
```

---

# 14. Layout-Elemente

## Text

```yaml
- type: text
  x: 10
  y: 20
  value: "{station}"
  font: Google Sans
  fontSize: 18
  color: "#FFFFFF"
```

## Rectangle

```yaml
- type: rectangle
  x: 0
  y: 0
  width: fill
  height: 40
  color: "#1A1C6B"
```

## Separator

```yaml
- type: separator
  x: 0
  y: 40
  width: fill
  thickness: 2
  color: "#FFFFFF"
```

## List

```yaml
- type: list
```

## Table

```yaml
- type: table
```

---

# 15. Unterschiede zwischen Table und List

## Table (`type: table`)

Empfohlener Standard für fast alle Anzeigetafeln.

Vorteile:

* automatische Kopfzeile mit `header:`
* Spalten werden automatisch nebeneinander angeordnet
* genau eine Spalte kann mit `width: fill` den restlichen Platz einnehmen
* Layout passt sich automatisch an unterschiedliche Displaygrößen an
* deutlich einfacher zu konfigurieren

Geeignet für:

* klassische Bahnsteiganzeigen
* Bahnhofsübersichten
* Flughafen- oder Busanzeigen

## List (`type: list`)

Jede Spalte bekommt eine feste `x`-Position.

Vorteile:

* maximale Kontrolle über jede Position
* ideal für pixelgenaue Speziallayouts

Nachteile:

* alle Positionen müssen manuell gesetzt werden
* bei anderen Displaygrößen oft Anpassungen nötig

Geeignet für:

* Sonderlayouts
* ältere bestehende Layouts
* sehr individuelle Designs

## Empfehlung

Für neue Layouts sollte fast immer `type: table` verwendet werden.

---

# 16. Tabellen (`type: table`)

Automatische Spaltenberechnung.

## Beispiel

```yaml
- type: table
  x: 0
  y: 48
  width: fill
  rowHeight: auto
  maxRows: 18
  zebra: "#1A1C6B,#3A3DB0"
  font: Google Sans
  columns:
    - header: "Abfahrt"
      width: 80
      value: "{time}"

    - header: "Über"
      width: fill
      value: "{via}"
      scroll: continuous

    - header: "Ziel"
      width: 100
      value: "{destination}"

    - header: "Gleis"
      width: 50
      value: "{track}"
      align: center
```

---

## Header

Die Eigenschaft `header:` erzeugt automatisch eine Kopfzeile.

---

## `width: fill`

Genau eine Spalte kann den verbleibenden Platz einnehmen.

---

# 17. Listen (`type: list`)

Absolute Positionierung jeder Spalte.

```yaml
- type: list
  columns:
    - value: "{time}"
      x: 8
      width: 50
```

---

# 18. Variants

Erlaubt alternative Darstellungen je nach Bedingung.

## Beispiel

```yaml
- header: "Abfahrt"
  width: 80
  variants:
    - value: "{time}"
      showWhen: no_arrival

    - value: "In Kürze"
      showWhen: on_arrival
      blink: true
      blinkTicks: 13
```

---

# 19. Sortierung (`sortBy`) (`sortBy`)

## Beispiel

```yaml
sortBy: time asc
```

Mögliche Felder:

* `time`
* `expected`
* `line`
* `destination`
* `track`
* `delayMinutes`

Richtungen:

* `asc`
* `desc`

---

# 20. Sounds

## Platform Sound

Wird an allen passenden SoundBoxen abgespielt.

## Train Sound

Wird direkt für alle Spieler im Zug abgespielt.

---

# 21. TrainCarts-Trigger

## Ankunftsschild

```text
[train]
ankunft
München Hbf:10
```

Sobald ein Zug dieses Schild überfährt:

* auf der Anzeige erscheint „In Kürze"
* der Bahnhofssound wird abgespielt
* die Zugdurchsage wird im Zug abgespielt

### Endbahnhof (`departures: final`)

* Kein `on_arrival`
* Sounds werden trotzdem abgespielt

---

## Abfahrtsschild

```text
[train]
abfahrt
München Hbf:10
```

Sobald ein Zug dieses Schild überfährt:

* die aktuelle Abfahrt wird als erledigt markiert
* der Zug verschwindet von der Anzeige

---

# 22. Runtime-State

Das Plugin speichert temporär:

* Verarbeitete Abfahrten
* Aktive Ankünfte

Dadurch werden:

* abgefahrene Züge nicht erneut angezeigt
* `on_arrival` korrekt gesetzt

---

# 23. Vollständige Praxisbeispiele

## Beispiel 1: Minimalistische Bahnsteiganzeige

```yaml
- type: table
  x: 0
  y: 40
  width: fill
  maxRows: 8
  columns:
    - header: "Abfahrt"
      width: 80
      value: "{time}"

    - header: "Ziel"
      width: fill
      value: "{destination}"

    - header: "Gleis"
      width: 50
      value: "{track}"
      align: center
```

## Beispiel 2: Anzeige mit „In Kürze"

```yaml
- header: "Abfahrt"
  width: 90
  variants:
    - value: "{time}"
      showWhen: no_arrival

    - value: "In Kürze"
      showWhen: on_arrival
      blink: true
      blinkTicks: 13
```

## Beispiel 3: Verspätungsanzeige

```yaml
- header: "Erwartet"
  width: 90
  variants:
    - value: "{expected}"
      showWhen: has_delay

    - value: "pünktlich"
      showWhen: no_delay
```

## Beispiel 4: Warnsymbol

```yaml
- header: "Info"
  width: 50
  align: center
  variants:
    - value: " "
      showWhen: no_warnings

    - value: "!"
      showWhen: has_warnings
      background: "#FFFFFF"
      color: "#1A1C6B"
```

## Beispiel 5: Bahnhofsübersicht

```yaml
- type: table
  source: stationDepartures
  width: fill
  maxRows: 20
  sortBy: time asc
  columns:
    - header: "Zeit"
      width: 80
      value: "{time}"

    - header: "Linie"
      width: 60
      value: "{line}"

    - header: "Ziel"
      width: fill
      value: "{destination}"

    - header: "Gleis"
      width: 60
      value: "{track}"
      align: center
```

## Beispiel 6: Vollständige Premium-Tabelle

```yaml
- type: table
  x: 0
  y: 48
  width: fill
  rowHeight: auto
  minRowHeight: 22
  maxRows: 18
  zebra: "#1A1C6B,#3A3DB0"
  font: Google Sans
  sortBy: time asc

  columns:
    - header: "Abfahrt"
      width: 80
      padding: 0 0 0 5
      variants:
        - value: "{time}"
          showWhen: no_arrival

        - value: "In Kürze"
          showWhen: on_arrival
          blink: true
          blinkTicks: 13

    - header: "Erwartet"
      width: 90
      variants:
        - value: "{expected}"
          showWhen: no_arrival

        - value: ""
          showWhen: on_arrival

    - header: "Linie"
      width: 50
      value: "{line}"

    - header: "Über"
      width: fill
      value: "{via}"
      scroll: continuous
      padding: 0 5 0 0

    - header: "Ziel"
      width: 100
      value: "{destination}"
      scroll: pingpong

    - header: "Gleis"
      width: 50
      value: "{track}"
      align: center

    - header: "Info"
      width: 50
      align: center
      variants:
        - value: " "
          showWhen: no_warnings

        - value: "{warnings}"
          showWhen: has_warnings
          background: "#FFFFFF"
          color: "#1A1C6B"
```

---

# 24. Tipps und Best Practices

## Schriftarten prüfen

```text
/abfahrtstafel debug fonts
```

## Sound testen

```text
/abfahrtstafel debug soundbox 10 München Hbf
```

## Layout neu laden

```text
/abfahrtstafel reload
```

## OGG-Dateien

Für räumliche Sounds sollten `.ogg`-Dateien als Mono-Dateien gespeichert sein.

## `width: fill`

Nur einmal pro Tabelle verwenden.

---
# Abfahrtstafel Plugin – Vollständige Dokumentation

## Inhaltsverzeichnis

1. Einführung
2. Schnellstart
3. Typische Einsatzszenarien
4. Voraussetzungen
5. Installation
6. Grundprinzip
7. Befehle
8. TrainLines.yml
9. SoundBoxes.yml
10. DisplayLayouts.yml
11. Placeholder
12. Bedingungen (`showWhen`)
13. Layout-Elemente
14. Tabellen (`type: table`)
15. Listen (`type: list`)
16. Variants
17. Sortierung (`sortBy`)
18. Sounds
19. TrainCarts-Trigger
20. Runtime-State
21. Vollständige Praxisbeispiele
22. Tipps und Best Practices

---

# 1. Einführung

Das Abfahrtstafel-Plugin simuliert dynamische Bahn-Anzeigen in Minecraft.

Es unterstützt:

* Abfahrtsanzeigen pro Gleis
* Gesamtübersichten für ganze Bahnhöfe
* Ankunftsstatus („In Kürze“)
* Verspätungen
* Warnhinweise
* Bahnhofsdurchsagen
* Zug-interne Durchsagen
* Frei gestaltbare Layouts
* Eigene Schriftarten

Die Darstellung erfolgt auf Maps bzw. Displays.

---

# 2. Schnellstart

## Ziel

Wir möchten eine Anzeigetafel für `München Hbf`, Gleis `10`, die die nächsten Abfahrten des ICE60 anzeigt und bei Einfahrt automatisch „In Kürze“ einblendet.

## Schritt 1: Zuglinie definieren

```yaml
trainLines:
  - name: ICE60
    description: München nach Hamburg
    orderedRailGroups:
      - orderIndex: 1
        name: "10"
        parentStation: München Hbf
        departures: 08:00,09:00,10:00
        arrivalPlatformSound: timberrail:an_muc
        arrivalTrainSound: timberrail:ice60_muc

      - orderIndex: 2
        name: "5"
        parentStation: Nürnberg Hbf
        departures: 09:05,10:05,11:05

      - orderIndex: 3
        name: "final"
        parentStation: Hamburg Hbf
        departures: final
```

## Schritt 2: SoundBox erstellen

```text
/abfahrtstafel soundbox create 10 München Hbf
```

## Schritt 3: Layout verwenden

```yaml
- type: table
  width: fill
  maxRows: 10
  columns:
    - header: "Abfahrt"
      width: 80
      value: "{time}"

    - header: "Ziel"
      width: fill
      value: "{destination}"

    - header: "Gleis"
      width: 50
      value: "{track}"
```

## Schritt 4: Trigger-Schilder setzen

### Ankunft

```text
[train]
ankunft
München Hbf:10
```

### Abfahrt

```text
[train]
abfahrt
München Hbf:10
```

## Schritt 5: Konfiguration laden

```text
/abfahrtstafel reload
```

---

# 3. Typische Einsatzszenarien

## Bahnsteiganzeige

Zeigt nur die nächsten Abfahrten eines bestimmten Gleises.

## Bahnhofsübersicht

Zeigt die nächsten Abfahrten aller Gleise eines Bahnhofs.

## Metro-/S-Bahn-Anzeige

Mit kurzen Zeilen und großen Schriften.

## Informationsdisplay

Mit Warnhinweisen und Sondermeldungen.

---

# 4. Voraussetzungen

* Paper
* BKCommonLib
* Train_Carts
* Optional: Resourcepack für eigene Sounds

Getestet wurde das Plugin bisher mit:

* Train_Carts: `v1.21.11-v2` (Build `1686`)
* BKCommonLib: `v1.21.11-v1` (Build `1957`)
* Server: `Paper 1.21.11-127-bd74vf6` (Minecraft `1.21.11`)

---

# 5. Installation

1. `Abfahrtstafel.jar` in den Ordner `plugins/` kopieren
2. Server starten
3. Konfigurationsdateien werden erstellt:

   * `config.yml`
   * `TrainLines.yml`
   * `SoundBoxes.yml`
   * `DisplayLayouts.yml`
4. Dateien anpassen
5. `/abfahrtstafel reload`

---

# 6. Grundprinzip

## Zuglinien

In `TrainLines.yml` definierst du:

* Linienname
* Reihenfolge der Stationen
* Gleise
* Abfahrtszeiten
* Sounds

## Displays

In `DisplayLayouts.yml` definierst du:

* Positionen
* Farben
* Schriftarten
* Tabellen
* Listen
* Inhalte und Bedingungen

## SoundBoxes

In `SoundBoxes.yml` definierst du:

* Ort des Lautsprechers
* Radius
* Lautstärke

## Trigger

Train_Carts-Schilder lösen aus:

* Ankunft
* Abfahrt

---

# 7. Befehle

## Allgemein

```text
/abfahrtstafel reload
```

Lädt alle Konfigurationen neu.

```text
/abfahrtstafel help
```

Zeigt Hilfe.

---

## Debug

```text
/abfahrtstafel debug fonts
```

Zeigt verfügbare Schriftarten.

```text
/abfahrtstafel debug soundbox <Gleis> <Bahnhof>
```

Spielt Testton an SoundBoxen.

```text
/abfahrtstafel debug trigger <Gleis> <Bahnhof>
```

Simuliert Trigger.

```text
/abfahrtstafel debug clearstate
```

Löscht verarbeitete Zustände.

---

## SoundBox-Verwaltung

```text
/abfahrtstafel soundbox create <Gleis> <Bahnhof>
```

Erstellt SoundBox an Spielerposition.

```text
/abfahrtstafel soundbox remove <ID>
```

Löscht SoundBox.

```text
/abfahrtstafel soundbox list
```

Listet SoundBoxen auf.

---

# 8. TrainLines.yml

Definiert Linien, Stationen und Fahrpläne.

## Beispiel

```yaml
trainLines:
  - name: ICE60
    description: München nach Hamburg
    orderedRailGroups:
      - orderIndex: 1
        name: "10"
        parentStation: München Hbf
        departures: 08:00,09:00,10:00
        arrivalPlatformSound: timberrail:an_muc
        arrivalTrainSound: timberrail:ice60_muc

      - orderIndex: 2
        name: "5"
        parentStation: Nürnberg Hbf
        departures: 09:05,10:05,11:05

      - orderIndex: 3
        name: "final"
        parentStation: Hamburg Hbf
        departures: final
        arrivalPlatformSound: timberrail:an_ham
```

---

## Felder

### `name`

Linienname.

### `description`

Beschreibung.

### `orderedRailGroups`

Liste der Stationen.

### `orderIndex`

Reihenfolge der Station.

### `name`

Gleisnummer.

### `parentStation`

Bahnhofsname.

### `departures`

Kommagetrennte Zeiten.

Oder:

```yaml
departures: final
```

für Endbahnhof.

### `arrivalPlatformSound`

Sound an der Station.

### `arrivalTrainSound`

Sound im Zug.

---

# 9. SoundBoxes.yml

Definiert Lautsprecherpositionen.

## Beispiel

```yaml
soundBoxes:
  - id: 1
    station: München Hbf
    railGroup: "10"
    world: world
    x: 100.5
    y: 65.0
    z: 200.5
    radius: 25.0
    volume: 1.0
    pitch: 1.0
```

---

## Felder

### `radius`

Reichweite in Blöcken.

### `volume`

Lautstärke.

### `pitch`

Tonhöhe.

---

# 10. DisplayLayouts.yml

Definiert das Erscheinungsbild.

## Grundstruktur

```yaml
layouts:
  - name: standard
    width: 512
    height: 384
    background: "#000000"
    elements:
      - type: text
        x: 10
        y: 20
        value: "{station}"
```

---

# 11. Placeholder

| Placeholder      | Bedeutung             |
| ---------------- | --------------------- |
| `{station}`      | Bahnhof               |
| `{railGroup}`    | Gleis                 |
| `{line}`         | Linie                 |
| `{time}`         | Sollzeit              |
| `{expected}`     | Erwartete Zeit        |
| `{delay}`        | Verspätung            |
| `{delayMinutes}` | Verspätung in Minuten |
| `{destination}`  | Ziel                  |
| `{via}`          | Zwischenhalte         |
| `{track}`        | Gleis                 |
| `{warnings}`     | Warnhinweise          |

---

# 12. Bedingungen (`showWhen`)

| Wert            | Bedeutung               |
| --------------- | ----------------------- |
| `has_departure` | Es gibt Abfahrten       |
| `no_departure`  | Keine Abfahrten         |
| `has_delay`     | Verspätung vorhanden    |
| `no_delay`      | Keine Verspätung        |
| `has_via`       | Zwischenhalte vorhanden |
| `no_via`        | Keine Zwischenhalte     |
| `has_warnings`  | Warnungen vorhanden     |
| `no_warnings`   | Keine Warnungen         |
| `on_arrival`    | Zug fährt ein           |
| `no_arrival`    | Kein Ankunftsstatus     |

Mehrere Bedingungen:

```yaml
showWhen: has_departure,no_delay
```

---

# 13. Layout-Elemente

## Text

```yaml
- type: text
  x: 10
  y: 20
  value: "{station}"
  font: Google Sans
  fontSize: 18
  color: "#FFFFFF"
```

## Rectangle

```yaml
- type: rectangle
  x: 0
  y: 0
  width: fill
  height: 40
  color: "#1A1C6B"
```

## Separator

```yaml
- type: separator
  x: 0
  y: 40
  width: fill
  thickness: 2
  color: "#FFFFFF"
```

## List

```yaml
- type: list
```

## Table

```yaml
- type: table
```

---

# 14. Unterschiede zwischen Table und List

## Table (`type: table`)

Empfohlener Standard für fast alle Anzeigetafeln.

Vorteile:

* automatische Kopfzeile mit `header:`
* Spalten werden automatisch nebeneinander angeordnet
* genau eine Spalte kann mit `width: fill` den restlichen Platz einnehmen
* Layout passt sich automatisch an unterschiedliche Displaygrößen an
* deutlich einfacher zu konfigurieren

Geeignet für:

* klassische Bahnsteiganzeigen
* Bahnhofsübersichten
* Flughafen- oder Busanzeigen

## List (`type: list`)

Jede Spalte bekommt eine feste `x`-Position.

Vorteile:

* maximale Kontrolle über jede Position
* ideal für pixelgenaue Speziallayouts

Nachteile:

* alle Positionen müssen manuell gesetzt werden
* bei anderen Displaygrößen oft Anpassungen nötig

Geeignet für:

* Sonderlayouts
* ältere bestehende Layouts
* sehr individuelle Designs

## Empfehlung

Für neue Layouts sollte fast immer `type: table` verwendet werden.

---

# 15. Tabellen (`type: table`) (`type: table`)

Automatische Spaltenberechnung.

## Beispiel

```yaml
- type: table
  x: 0
  y: 48
  width: fill
  rowHeight: auto
  maxRows: 18
  zebra: "#1A1C6B,#3A3DB0"
  font: Google Sans
  columns:
    - header: "Abfahrt"
      width: 80
      value: "{time}"

    - header: "Über"
      width: fill
      value: "{via}"
      scroll: continuous

    - header: "Ziel"
      width: 100
      value: "{destination}"

    - header: "Gleis"
      width: 50
      value: "{track}"
      align: center
```

---

## Header

Die Eigenschaft `header:` erzeugt automatisch eine Kopfzeile.

---

## `width: fill`

Genau eine Spalte kann den verbleibenden Platz einnehmen.

---

# 16. Listen (`type: list`) (`type: list`)

Absolute Positionierung jeder Spalte.

```yaml
- type: list
  columns:
    - value: "{time}"
      x: 8
      width: 50
```

---

# 17. Variants

Erlaubt alternative Darstellungen je nach Bedingung.

## Beispiel

```yaml
- header: "Abfahrt"
  width: 80
  variants:
    - value: "{time}"
      showWhen: no_arrival

    - value: "In Kürze"
      showWhen: on_arrival
      blink: true
      blinkTicks: 13
```

---

# 18. Sortierung (`sortBy`)

## Beispiel

```yaml
sortBy: time asc
```

Mögliche Felder:

* `time`
* `expected`
* `line`
* `destination`
* `track`
* `delayMinutes`

Richtungen:

* `asc`
* `desc`

---

# 19. Sounds

## Platform Sound

Wird an allen passenden SoundBoxen abgespielt.

## Train Sound

Wird direkt für alle Spieler im Zug abgespielt.

---

# 20. TrainCarts-Trigger

## Ankunftsschild

```text
[train]
ankunft
München Hbf:10
```

Sobald ein Zug dieses Schild überfährt:

* auf der Anzeige erscheint „In Kürze"
* der Bahnhofssound wird abgespielt
* die Zugdurchsage wird im Zug abgespielt

### Endbahnhof (`departures: final`)

* Kein `on_arrival`
* Sounds werden trotzdem abgespielt

---

## Abfahrtsschild

```text
[train]
abfahrt
München Hbf:10
```

Sobald ein Zug dieses Schild überfährt:

* die aktuelle Abfahrt wird als erledigt markiert
* der Zug verschwindet von der Anzeige

---

# 21. Runtime-State

Das Plugin speichert temporär:

* Verarbeitete Abfahrten
* Aktive Ankünfte

Dadurch werden:

* abgefahrene Züge nicht erneut angezeigt
* `on_arrival` korrekt gesetzt

---

# 22. Vollständige Praxisbeispiele

## Beispiel 1: Minimalistische Bahnsteiganzeige

```yaml
- type: table
  x: 0
  y: 40
  width: fill
  maxRows: 8
  columns:
    - header: "Abfahrt"
      width: 80
      value: "{time}"

    - header: "Ziel"
      width: fill
      value: "{destination}"

    - header: "Gleis"
      width: 50
      value: "{track}"
      align: center
```

## Beispiel 2: Anzeige mit „In Kürze"

```yaml
- header: "Abfahrt"
  width: 90
  variants:
    - value: "{time}"
      showWhen: no_arrival

    - value: "In Kürze"
      showWhen: on_arrival
      blink: true
      blinkTicks: 13
```

## Beispiel 3: Verspätungsanzeige

```yaml
- header: "Erwartet"
  width: 90
  variants:
    - value: "{expected}"
      showWhen: has_delay

    - value: "pünktlich"
      showWhen: no_delay
```

## Beispiel 4: Warnsymbol

```yaml
- header: "Info"
  width: 50
  align: center
  variants:
    - value: " "
      showWhen: no_warnings

    - value: "!"
      showWhen: has_warnings
      background: "#FFFFFF"
      color: "#1A1C6B"
```

## Beispiel 5: Bahnhofsübersicht

```yaml
- type: table
  source: stationDepartures
  width: fill
  maxRows: 20
  sortBy: time asc
  columns:
    - header: "Zeit"
      width: 80
      value: "{time}"

    - header: "Linie"
      width: 60
      value: "{line}"

    - header: "Ziel"
      width: fill
      value: "{destination}"

    - header: "Gleis"
      width: 60
      value: "{track}"
      align: center
```

## Beispiel 6: Vollständige Premium-Tabelle

```yaml
- type: table
  x: 0
  y: 48
  width: fill
  rowHeight: auto
  minRowHeight: 22
  maxRows: 18
  zebra: "#1A1C6B,#3A3DB0"
  font: Google Sans
  sortBy: time asc

  columns:
    - header: "Abfahrt"
      width: 80
      padding: 0 0 0 5
      variants:
        - value: "{time}"
          showWhen: no_arrival

        - value: "In Kürze"
          showWhen: on_arrival
          blink: true
          blinkTicks: 13

    - header: "Erwartet"
      width: 90
      variants:
        - value: "{expected}"
          showWhen: no_arrival

        - value: ""
          showWhen: on_arrival

    - header: "Linie"
      width: 50
      value: "{line}"

    - header: "Über"
      width: fill
      value: "{via}"
      scroll: continuous
      padding: 0 5 0 0

    - header: "Ziel"
      width: 100
      value: "{destination}"
      scroll: pingpong

    - header: "Gleis"
      width: 50
      value: "{track}"
      align: center

    - header: "Info"
      width: 50
      align: center
      variants:
        - value: " "
          showWhen: no_warnings

        - value: "{warnings}"
          showWhen: has_warnings
          background: "#FFFFFF"
          color: "#1A1C6B"
```

---

# 23. Tipps und Best Practices

## Schriftarten prüfen

```text
/abfahrtstafel debug fonts
```

## Sound testen

```text
/abfahrtstafel debug soundbox 10 München Hbf
```

## Layout neu laden

```text
/abfahrtstafel reload
```

## OGG-Dateien

Für räumliche Sounds sollten `.ogg`-Dateien als Mono-Dateien gespeichert sein.

## `width: fill`

Nur einmal pro Tabelle verwenden.

---
