# Abfahrtstafel -- Benutzeranleitung

Dieses Plugin bringt **Zugabfahrtstafeln** auf deinen Minecraft-Server.
Es zeigt Abfahrten, Ankünfte und Verspätungen auf Item-Frame-Displays
an, spielt Bahnhofsansagen ab und kann über Schilder mit
**TrainCarts**-Zügen verbunden werden.

## Inhalt

1.  Voraussetzungen & Installation
2.  Berechtigungen
3.  Grundprinzip
4.  Fahrpläne -- `TrainLines.yml`
5.  Stationsaliase -- `StationAliases.yml`
6.  Schilder für Abfahrt/Ankunft
7.  Anzeigen platzieren & entfernen
8.  Layouts -- `DisplayLayouts.yml`
9.  Soundboxen & Ansagen
10. Warnmeldungen
11. Befehlsübersicht
12. `config.yml`

------------------------------------------------------------------------

## 1. Voraussetzungen & Installation

Benötigt wird ein Spigot/Paper-Server (API-Version 1.21) mit
**BKCommonLib** und **TrainCarts**.

1.  `BKCommonLib` und `Train_Carts` sowie `Abfahrtstafel.jar` in den
    `plugins`-Ordner legen.
2.  Server starten.
3.  Es wird automatisch der Ordner `plugins/Abfahrtstafel/` mit
    `config.yml`, `TrainLines.yml`, `StationAliases.yml`,
    `DisplayLayouts.yml`, `SoundBoxes.yml`, `SoundMessages.yml` und
    `WarnMessages.yml` angelegt.
4.  Dateien anpassen, dann `/abfahrtstafel reload`.

## 2. Berechtigungen

  ------------------------------------------------------------------------
  Node                     Wirkung                 Standard
  ------------------------ ----------------------- -----------------------
  `abfahrtstafel.admin`    Vollzugriff (Displays,  OP
                           Layouts, Soundboxen,    
                           Debug)                  

  `abfahrtstafel.warn`     Warnmeldungen verwalten OP

  `abfahrtstafel.reload`   Konfiguration neu laden OP
  ------------------------------------------------------------------------

## 3. Grundprinzip

1.  **Fahrplan** in `TrainLines.yml` definieren (Linie, Stationen,
    Gleise, Zeiten).
2.  **Schilder** an den Gleisen platzieren, die TrainCarts das Auslösen
    von Abfahrt/Ankunft mitteilen.
3.  **Anzeige** mit `/abfahrtstafel place` an einer Station/einem Gleis
    platzieren -- sie aktualisiert sich danach automatisch.

------------------------------------------------------------------------

## 4. Fahrpläne -- `TrainLines.yml`

``` yaml
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

  -----------------------------------------------------------------------
  Feld                                Bedeutung
  ----------------------------------- -----------------------------------
  `name`                              Name der Linie (erscheint als
                                      `{line}`)

  `description`                       Freitext, rein informativ

  `orderedRailGroups`                 Die Stationen/Gleise in
                                      Fahrtreihenfolge

  `orderIndex`                        Reihenfolge der Haltestelle

  `name` (in `orderedRailGroups`)     Gleisbezeichnung

  `parentStation`                     Stationsname (voll oder als Alias,
                                      siehe Kapitel 5)

  `departures`                        Kommagetrennte Zeiten (`H:MM`) oder
                                      `final` für den Endhalt

  `arrivalPlatformSound` /            Sound bei Einfahrt am Gleis bzw. im
  `arrivalTrainSound` *(optional)*    Zug
  -----------------------------------------------------------------------

## 5. Stationsaliase -- `StationAliases.yml`

``` yaml
aliases:
  muc: München Hbf
  ts: Stuttgart Hbf
```

Aliase kannst du überall statt des vollen Stationsnamens verwenden --
auf Schildern, in Befehlen und beim Platzieren von Anzeigen.

## 6. Schilder für Abfahrt/Ankunft

    [train]
    abfahrt
    München Hbf:10

    [train]
    ankunft
    München Hbf:10

-   Zeile 2: `abfahrt` oder `ankunft`.
-   Zeile 3: `Station:Gleis` (Alias möglich).

**Ankunftsschild:** Sobald ein Zug darüberfährt, erscheint auf der
Anzeige der Ankunftsstatus (`on_arrival`, z. B. „In Kürze"), der
Bahnsteig-Sound wird an allen passenden Soundboxen abgespielt und der
Zug-Sound direkt im Zug. Bei einem Endhalt (`departures: final`) gibt es
keinen `on_arrival`-Status, die Sounds werden aber trotzdem abgespielt.

**Abfahrtsschild:** Die aktuelle Abfahrt wird als erledigt markiert und
verschwindet von der Anzeige -- die nächste Abfahrt rückt nach.

Mit `/abfahrtstafel debug signactions true` bzw.
`debugSignActions: true` in `config.yml` siehst du in der Konsole live,
welches Schild wann auslöst.

## 7. Anzeigen platzieren & entfernen

    /abfahrtstafel place <Layout> <normal|glow> <Station>
    /abfahrtstafel place <Layout> <normal|glow> <Gleis> <Station>

Auf den Zielblock schauen, dann den Befehl ausführen. Ohne Gleisangabe
zeigt die Anzeige alle Gleise der Station (Stationstafel), mit
Gleisangabe nur dieses eine Gleis (Bahnsteigtafel).

    /abfahrtstafel place platform-small normal 10 München Hbf

Entfernen: Auf einen Rahmen der Anzeige schauen und
`/abfahrtstafel remove` ausführen -- alle zusammenhängenden Rahmen
werden automatisch erkannt und entfernt.

------------------------------------------------------------------------

## 8. Layouts -- `DisplayLayouts.yml`

Ein Layout beschreibt, wie eine Anzeige aussieht: Größe, Hintergrund und
eine Liste von **Elementen**.

``` yaml
layouts:
  platform-small:
    displayType: platform          # platform (ein Gleis) oder station (ganze Station)
    widthBlocks: 1
    heightBlocks: 2
    background: "#2B2D8D"
    elements:
      - type: text
        value: "{line}"
        x: 10
        y: 22
        width: 40
        fontSize: 16
        color: "#FFFFFF"
```

### 8.1 Platzhalter

Für einzelne Abfahrten/Ankünfte in `value` nutzbar:

  -----------------------------------------------------------------------
  Platzhalter                         Bedeutung
  ----------------------------------- -----------------------------------
  `{station}`                         Bahnhof

  `{railGroup}` / `{track}`           Gleis

  `{line}`                            Linie

  `{time}`                            Sollzeit

  `{expected}`                        Erwartete Zeit (bei Verspätung)

  `{delay}` / `{delayMinutes}`        Verspätung

  `{destination}`                     Ziel

  `{via}`                             Zwischenhalte

  `{arrivalTime}` / `{departureTime}` Ankunfts-/Abfahrtszeit

  `{warnings}`                        Aktive Warnmeldungen (mehrere
                                      werden mit `***` getrennt)
  -----------------------------------------------------------------------

### 8.2 Elementtypen

-   **`text`** -- einfacher Textblock an Position `x`/`y`.
-   **`rectangle`** -- gefüllte Fläche, z. B. als Hintergrundbalken.
-   **`separator`** -- eine Trennlinie (`thickness`).
-   **`list`** -- Spalten mit fester `x`-Position, volle Kontrolle über
    jede Position, aber jede Anpassung ist manuell.
-   **`table`** -- Spalten mit `header:` und automatischer Anordnung;
    **empfohlener Standard** für die meisten Anzeigetafeln (passt sich
    automatisch an unterschiedliche Displaygrößen an). Genau eine Spalte
    kann `width: fill` bekommen, die den Restplatz einnimmt.

Für Sonderlayouts mit pixelgenauer Positionierung eignet sich `list`,
für klassische Bahnsteig-/Bahnhofstafeln fast immer `table`.

### 8.3 Bedingungen -- `showWhen`

Elemente/Spalten/Varianten lassen sich abhängig vom Zustand ein- oder
ausblenden:

  -----------------------------------------------------------------------
  Wert                                Bedeutung
  ----------------------------------- -----------------------------------
  `has_departure` / `no_departure`    Abfahrt(en) vorhanden / keine

  `has_delay` / `no_delay`            Verspätung vorhanden / keine

  `has_via` / `no_via`                Zwischenhalte vorhanden / keine

  `has_warnings` / `no_warnings`      Warnungen vorhanden / keine

  `on_arrival` / `no_arrival`         Zug fährt gerade ein / kein
                                      Ankunftsstatus

  `on_demand` / `no_ondemand`         Fahrt ist Bedarfshalt / ist kein
                                      Bedarfshalt
  -----------------------------------------------------------------------

Mehrere Bedingungen kommagetrennt (alle müssen zutreffen):
`showWhen: has_departure,no_delay`

### 8.4 Tabellen (`type: table`)

``` yaml
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
  source: stationDepartures    # optional, z. B. für Stationsübersichten
  limit: 20                    # optional, max. Zeilenzahl der Quelle

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

Wichtig zu wissen:

-   **`header`** erzeugt automatisch eine Kopfzeile.
-   **`width: fill`** darf pro Tabelle nur bei genau einer Spalte
    gesetzt werden.
-   **`zebra`**: zwei Farben, die für abwechselnde Zeilenhintergründe
    genutzt werden.
-   **`rowHeight: auto`** passt die Zeilenhöhe automatisch an,
    `minRowHeight` setzt dabei eine Mindesthöhe.
-   **`scroll`**: `continuous` (durchgehend laufend) oder `pingpong`
    (hin und her), wenn der Text breiter ist als die Spalte.
-   **`variants`**: erlaubt, je nach `showWhen` einen anderen Wert (und
    Stil) für dieselbe Spalte anzuzeigen -- z. B. Zeit vs. „In Kürze",
    oder ein Warnsymbol nur wenn Warnungen aktiv sind.
-   **`sortBy`**: Feld + Richtung, z. B. `time asc` oder
    `delayMinutes desc`. Sortierbare Felder: `time`, `expected`, `line`,
    `destination`, `track`, `delayMinutes`.

### 8.5 Listen (`type: list`)

``` yaml
- type: list
  columns:
    - value: "{time}"
      x: 8
      width: 50
```

Jede Spalte bekommt eine feste `x`-Position statt automatischer
Anordnung -- dafür musst du bei geänderter Displaygröße alle Positionen
selbst nachziehen.

### 8.6 Eigene Layouts

Ein neues Layout ist einfach ein weiterer Eintrag unter `layouts:` in
`DisplayLayouts.yml`. Nach dem Speichern reicht `/abfahrtstafel reload`.
Mit `/abfahrtstafel layouts` siehst du alle vorhandenen Layouts
inklusive Typ und Größe im Spiel, mit `/abfahrtstafel debug fonts` alle
auf dem Server verfügbaren Schriftarten (relevant für `font:`).

------------------------------------------------------------------------

## 9. Soundboxen & Ansagen

**Soundboxen** (`SoundBoxes.yml`) sind ortsgebundene Lautsprecher:

``` yaml
soundBoxes:
  - station: "Timber Hbf"
    railGroup: "1a"
    world: "world"
    x: 100.5
    y: 65.0
    z: 200.5
    radius: 24
    volume: 1.0
    pitch: 1.0
```

Am schnellsten legst du eine Soundbox an deiner aktuellen Position per
Befehl an: `/abfahrtstafel soundbox create 1a Timber Hbf`. Werte lassen
sich pro Box überschreiben, Standardwerte kommen aus `config.yml`.

**Ansagen** (`SoundMessages.yml`) sind wiederkehrende Durchsagen:

``` yaml
soundMessages:
  - id: 1
    description: "Bitte zurückbleiben"
    enabled: true
    groups: Stuttgart Hbf:2,Stuttgart Hbf:16,München Hbf
    sound: minecraft:block.note_block.pling
    mode: interval
    intervalSeconds: 300

  - id: 3
    description: "Stündlicher Hinweis"
    enabled: true
    groups: Stuttgart Hbf
    sound: minecraft:block.note_block.pling
    mode: time
    times: 05:00,06:00,07:00
```

-   **groups**: `global`, eine Station, ein Gleis (`Station:Gleis`) oder
    mehrere davon kommagetrennt.
-   **mode**: `interval` (fester Abstand), `random` (zufällig zwischen
    `minIntervalSeconds`/`maxIntervalSeconds`) oder `time` (feste
    Uhrzeiten in `times`).
-   Umschaltbar per Befehl, ohne die Datei zu bearbeiten:
    `/abfahrtstafel soundmessage enable/disable <id>`.

## 10. Warnmeldungen -- `WarnMessages.yml`

Lauftext-Hinweise, die auf Anzeigen erscheinen (z. B. bei Störungen):

``` yaml
warnMessages:
  - id: 1
    message: Test Warnmeldung für "Stuttgart Hbf" Gleis "12" und Gleis "16"
    active: false
    groups: Stuttgart Hbf:12,Stuttgart Hbf:16

  - id: 2
    message: Globale Testwarnung
    active: false
    groups: global
```

`groups` funktioniert wie bei den Ansagen (`global`, Station,
`Station:Gleis` oder `lines:<Linienname>`). Ein-/Ausschalten im
laufenden Betrieb: `/abfahrtstafel warn enable/disable <id>`. Im Layout
wird eine aktive Warnung über den Platzhalter `{warnings}` bzw.
`showWhen: has_warnings` sichtbar gemacht (siehe Kapitel 8).

------------------------------------------------------------------------

## 11. Befehlsübersicht

Alle Befehle beginnen mit `/abfahrtstafel` (Berechtigung
`abfahrtstafel.admin`, sofern nicht anders angegeben) und unterstützen
Tab-Vervollständigung.

  --------------------------------------------------------------------------------------------------
  Befehl                                                         Beschreibung
  -------------------------------------------------------------- -----------------------------------
  `place <Layout> <normal\|glow> <Station>`                      Stationstafel platzieren

  `place <Layout> <normal\|glow> <Gleis> <Station>`              Bahnsteigtafel platzieren

  `remove`                                                       Anzeige entfernen (auf einen Rahmen
                                                                 schauen)

  `layouts`                                                      Verfügbare Layouts auflisten

  `trigger <Station>:<Gleis>`                                    Nächste Abfahrt manuell abarbeiten

  `reload`                                                       Konfiguration neu laden
                                                                 (`abfahrtstafel.reload`)

  `warn list / enable <id> / disable <id>`                       Warnmeldungen verwalten
                                                                 (`abfahrtstafel.warn`)

  `soundbox list / create <Gleis> <Bahnhof> / remove <id>`       Soundboxen verwalten

  `soundmessage list / play <id> / enable <id> / disable <id>`   Ansagen verwalten

  `debug signactions <true\|false>`                              Schild-Debug ein-/ausschalten

  `debug trigger <Station>:<Gleis>`                              Trigger testweise ausführen

  `debug soundbox <Gleis> <Bahnhof>`                             Testsound abspielen

  `debug fonts`                                                  Verfügbare Schriftarten auflisten

  `debug clearstate`                                             Gemerkte Abfahrten/Ankünfte
                                                                 zurücksetzen
  --------------------------------------------------------------------------------------------------

## 12. `config.yml`

  ---------------------------------------------------------------------------------------
  Einstellung                          Bedeutung                  Standard
  ------------------------------------ -------------------------- -----------------------
  `departureTimeoutMinutes`            Ab wann eine Abfahrt als   5
                                       verpasst gilt und          
                                       gewechselt wird            

  `displayUpdateTicks`                 Aktualisierungsintervall   10
                                       der Anzeigen               

  `textScrollSpeedTicks`               Lauftext-Geschwindigkeit   1

  `stationLookAheadMinutes` /          Vorschau-Zeitraum für      120 / -1
  `platformLookAheadMinutes`           Stations- bzw. Gleistafeln 
                                       (-1 = unbegrenzt)          

  `arrivalTriggerLookAheadMinutes`     Zeitfenster, in dem ein    1
                                       Ankunftsschild auslösen    
                                       darf                       

  `stationDisplayMaxEntries`           Max. Zeilen auf einer      12
                                       Stationstafel              

  `debugSignActions`                   Konsolen-Debug für         false
                                       Schilder                   

  `announcementSoundCategory`          Soundkategorie für Ansagen MASTER

  `soundBoxDefaultRadius` /            Standardwerte neuer        24 / 1.0 / 1.0
  `soundBoxDefaultVolume` /            Soundboxen                 
  `soundBoxDefaultPitch`                                          

  `soundMessagesInitialDelaySeconds`   Verzögerung der ersten     60
                                       Ansage nach Serverstart    
  ---------------------------------------------------------------------------------------

------------------------------------------------------------------------

------------------------------------------------------------------------

# Ergänzungen für Version 2

## Bedarfshalte (`ondemand`)

Ein Bedarfshalt wird in `TrainLines.yml` mit

``` yaml
departures: ondemand
```

definiert.

Eigenschaften:

-   dauerhaft sichtbar
-   besitzt keine feste Uhrzeit
-   wechselt nach einem Ankunftstrigger auf **„In Kürze"**
-   kehrt nach einem Abfahrtstrigger automatisch zu **„Bei Bedarf"**
    zurück
-   wird hinter regulären Abfahrten einsortiert

## Ergänzung zu `TrainLines.yml`

Für `departures` stehen drei Varianten zur Verfügung:

  Wert                Bedeutung
  ------------------- ------------------------
  `08:00,09:00,...`   Normale Abfahrtszeiten
  `ondemand`          Bedarfshalt
  `final`             Endhalt

`arrivalPlatformSound` wird beim Auslösen eines **Ankunftsschildes** an
allen passenden Soundboxen abgespielt.

`arrivalTrainSound` wird gleichzeitig direkt im auslösenden
TrainCarts-Zug abgespielt.

Beide Felder sind optional.

## Ergänzung: Filter (`filter`)

Mit `filter` können Listen und Tabellen auf bestimmte Abfahrten
eingeschränkt werden.

``` yaml
filter: no_ondemand
```

  Filter          Bedeutung
  --------------- -------------------------
  `on_demand`     Nur Bedarfshalte
  `no_ondemand`   Bedarfshalte ausblenden
  `has_delay`     Nur verspätete Züge
  `no_delay`      Nur pünktliche Züge

## Ergänzung: Varianten

`variants` können nahezu alle Eigenschaften eines Elements
überschreiben, beispielsweise:

-   value
-   color
-   background
-   font
-   fontSize
-   blink
-   scroll
-   padding

Dadurch kann dasselbe Element abhängig von `showWhen` unterschiedlich
dargestellt werden.

## Transparente Hintergründe

Farben können als `#RRGGBB` oder mit Alphakanal als `#RRGGBBAA`
angegeben werden.

Beispiel:

``` yaml
background: "#002B2D80"
```

## Schriftarten

Es können alle auf dem Server installierten Java-Schriftarten verwendet
werden.

Mit

``` text
/abfahrtstafel debug fonts
```

werden alle verfügbaren Schriftarten aufgelistet.

## sortBy

Syntax:

``` yaml
sortBy: <Feld> <Richtung>
```

Beispiele:

``` yaml
sortBy: time asc
sortBy: time desc
sortBy: destination asc
sortBy: delayMinutes desc
```
