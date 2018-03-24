# Allgemeines

Der Server läuft auf Port 7000.
Benutzung:

``` 
    java -jar server.jar <Ordner, in dem die hochgeladenen Bilder gespeichert werden>
```

# Datenbank

Damit der Server funktioniert, muss auf Port 3306 eine MySQL-Datenbank
mit dem Namen "hibernate" laufen.
Die Struktur der Datenbank ist zum Importieren in der .sql-Datei
hinterlegt.
Die Passwörter werden als SHA-512-Hash gespeichert.

# Datentypen
Momentan sind lediglich "height", "temperature", "latitude" und "longitude" valide Datentypen. Der Wert muss jeweils als Double vorliegen. 

# REST

## Zugangsdaten

Um auf die REST-API zugreifen zu können, müssen via HTTP-BasicAuth
Zugangsdaten mitgeschickt werden. Sind die Daten inkorrekt oder fehlen die nötigen Rechte, wird der Status-Code 401 zurückgesendet.

## GET-Requests

Für GET-Requests keine Berechtigungen/Zugangsdaten erforderlich. Liste aller
URLs:

  - [/picture](/picture) sendet das neueste Bild zurück

  - [/pictures/:id](/pictures/:id) sendet das Bild mit der ID :id zurück

  - [/pictures](/pictures) sendet eine Liste aller Bilder als JSON
    zurück

  - [/data?time=:time\&type=:type](/data?time=:time&type=:type) sendet
    eine Liste aller Messdaten vom Typ :type, die nach :time erstellt
    wurden, als JSON zurück. Werden einer oder mehrere der Parameter
    nicht gesetzt, werden sie nicht beachtet. :time hat das Format
    "yyyy-mm-dd hh:mm:ss".

## POST-Requests

Für POST-Requests sind Schreibrechte erforderlich. Liste aller URLs:

  - [/data?type=:type\&value=:value](/data?type=:type&value=:value) fügt
    ein Messdatum vom Typ :type mit Wert :value mit der aktuellen Zeit
    in die Datenbank ein

  - [/picture](/picture) lädt ein Bild hoch. Eine HTML-form, die das
    macht, muss enctype="multipart/form-data" als Attribut haben. Der
    Name des Dateiparameters in der Request muss "picture" lauten.

# WebSockets

## Authentifizierung

Da bei WebSockets die Authentifizierung über HTTP-BasicAuth nicht
möglich ist, wird hierfür ein Token benötigt. Diesen erhält durch eine GET-Request auf [/login](/login) mit den
üblichen HTTP-BasicAuth-Zugangsdaten.

## Senden

### Messdaten

Der Websocket zum Senden von Messdaten ist unter
[/sendData?token=:token](/sendData?token=:token) zu erreichen. :token
ist hierbei der Authentifizierungstoken. Das Protokoll ist nicht
[http(s)://](http\(s\)://), sondern <ws://>. Zum Senden von Messdaten
sind Schreibrechte erforderlich. Zum Senden muss eine Nachricht von
folgendem Format verschickt werden:

``` 
    <Datentyp> <Wert>
```

### Bilder

Der Websocket zum Senden von Bildern ist unter
[/sendPictures?token=:token](/sendPictures?token=:token) zu erreichen.
:token ist hierbei der Authentifizierungstoken. Das Protokoll ist nicht
[http(s)://](http\(s\)://), sondern <ws://>. Zum Senden von Bildern sind
Schreibrechte erforderlich. Das Bild wird binär versendet.

## Empfangen

Die Websockets zum Empfangen werden sofort nach Upload neuer Daten auf
den Server über die Daten benachrichtigt.

### Messdaten

Der Websocket zum Empfangen von Messdaten ist unter
[/receiveData](/receiveData) zu erreichen. Das Protokoll ist nicht
[http(s)://](http\(s\)://), sondern <ws://>. Zum Empfangen von Messdaten
keine Rechte erforderlich. Die vom Server gesendeten Nachrichten
haben folgendes Format:

``` 
    <Datentyp> <Wert>
```

### Bilder

Der Websocket zum Empfangen von Bildern ist unter
[/receivePictures](/receivePictures) zu
erreichen. Das Protokoll ist nicht [http(s)://](http\(s\)://), sondern <ws://>. Zum Empfangen von
Bildern sind keine Rechte erforderlich. Eine Nachricht im WebSocket enthält Bild-ID, Datum und Dateityp als JSON.
