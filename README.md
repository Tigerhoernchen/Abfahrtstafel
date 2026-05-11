
# Abfahrtstafel

Minecraft-Plugin für Paper/Spigot mit realistischen Bahnhofs- und Gleisanzeigen auf Karten.


https://github.com/user-attachments/assets/e3c69a36-87c4-447c-b46e-7f2b64b5c8e0



## Vorwort
> [!IMPORTANT]
> Das Plugin ist nicht komplett geprüft. <br>
> Der Code wurde Mithilfe von KI geschrieben. <br>
> Das Plugin dient primär zum eigenen Zweck. <br>
> Es können jederzeit Änderungen, die das löschen alter Konfigurationsdateien vorsieht, erscheinen. (Vorher Sichern und danach neu implementieren, danke) <br>
> Die Bildschirme sind der DB InfraGo nachempfunden: www.dbinfrago.com/resource/blob/13207208/0664f25800cfddb6e95657d09cf8bc6b/Stationsnutzung_Monitore-data.pdf

## Download
Ich versuche die .jar dazu immer aktuell zu halten. [Diese findest du hier](Abfahrtstafel/target)

## Funktionen



- Große Bahnhofsanzeigen mit mehreren Abfahrten
- Kleine Gleisanzeigen im Stil moderner DB-Monitore
- Fahrpläne über `TrainLines.yml`
- Warnmeldungen über `WarnMessages.yml`
- Trigger per Command oder TrainCarts-Schild
- Verspätungs- und Überfälligkeitsanzeige
- Lauftexte für lange Texte
- Automatische Mitternachtsbehandlung

# Voraussetzungen

- Paper Server
- BKCommonLib
- TrainCarts (optional, für Trigger-Schilder)

# Befehle

## Display erstellen (manuell)
### Große Bahnhofsanzeige (größe Variabel)
```
/abfahrtstafel give station <Stationsname>
```
### Kleine Gleisanzeige (1x2 Blöcke empfohlen)
```
/abfahrtstafel give platform <Gleis> <Stationsname>
```
## Display erstellen (automatisch)
### Bereich auswählen
```
/abfahrtstafel pos1
/abfahrtstafel pos2
```
### Display setzen
Die Displaygröße ist nicht begrenzt. Für den Bahnsteig wird eine Displaygröße von 1x2 Blöcken empfohlen.
```
/abfahrtstafel place station <normal/glow> <Bahnhof>
/abfahrtstafel place platform <normal/glow> <Gleis> <Bahnhof>
```
## Display entfernen
### Das Display ansehen
```
/abfahrtstafel remove
```
## Trigger einer Abfahrt
```
/abfahrtstafel trigger <Station>:<Gleis>
```
## Warnmeldungen
```
/abfahrtstafel warn list
/abfahrtstafel warn enable <id>
/abfahrtstafel warn disable <id>
```

## Sonstige Befehle
```
/abfahrtstafel reload
/abfahrtstafel clearstate
```
# TrainCarts Trigger-Schild
> [!NOTE]
> Dieses Funktion wurde noch nicht ausgiebig getestet.
```
[train]
Abfahrt
Timber Hbf:1b
```

# TrainLines.yml
> [!NOTE]
> Dieses Funktion wurde noch nicht ausgiebig getestet. <br>
> Eingabe diverser/falscher Werte wurde noch nicht ausgiebig getestet.
```
trainLines:
  - name: <Linienname>
    description: <beschreibung>
    orderedRailGroups:
      - orderIndex: <Indexnummer>
        name: <Gleis>
        parentStation: <Bahnhofsname>
        departures: <Abfahrtszeiten,Kommagetrennt>
      - orderIndex: <Indexnummer>
        name: <Gleis>
        parentStation: <Bahnhofsname>
        departures: <Abfahrtszeiten,Kommagetrennt>
      - orderIndex: <Indexnummer>
        name: <Gleis>
        parentStation: <Bahnhofsname>
        departures: final (Am Endbahnhof final statt Uhrzeiten)
```
```
trainLines:
  - name: ICE1
    description: ICE1_sued_nach_nord
    orderedRailGroups:
      - orderIndex: 1
        name: 12
        parentStation: München Hbf
        departures: 19:50,19:53,19:55

      - orderIndex: 2
        name: 2
        parentStation: Ulm
        departures: 20:00,20:03,20:05

      - orderIndex: 3
        name: 16
        parentStation: Stuttgart Hbf
        departures: final
  - name: ICE2
    description: ICE1_nord_nach_sued
    orderedRailGroups:
      - orderIndex: 1
        name: 12
        parentStation: Stuttgart Hbf
        departures: 19:50,19:53,19:55

      - orderIndex: 2
        name: 2
        parentStation: Ulm
        departures: 20:00,20:03,20:05

      - orderIndex: 3
        name: 16
        parentStation: München Hbf
        departures: final
```
# WarnMessages.yml
```
warnMessages:
  - id: 1
    message: Globale Testwarnung
    active: true
    groups: global

  - id: 2
    message: Hier könnte Ihre Warnung stehen
    active: true
    groups: Timber Hbf:1b
```

# Permissions
```
permissions:
  abfahrtstafel.admin:
    description: Vollzugriff auf alle Funktionen
    default: op

  abfahrtstafel.warn:
    description: Warnmeldungen anzeigen und verwalten
    default: op

  abfahrtstafel.reload:
    description: Konfiguration neu laden
    default: op
```
