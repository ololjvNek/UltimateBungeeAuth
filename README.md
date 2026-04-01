# UltimateBungeeAuth 2.x

Proxy-side authentication for **Waterfall / BungeeCord** and **Velocity**: MySQL storage, optional queue to the lobby, premium (online-mode) flow, rate limiting, and BCrypt passwords.

---

## Spis treści

1. [Wymagania](#wymagania)
2. [Instalacja (świeża)](#instalacja-świeża)
3. [Aktualizacja z wersji 1.x](#aktualizacja-z-wersji-1x) — **ważne dla administratorów**
4. [Pliki JAR](#pliki-jar)
5. [Konfiguracja (skrót)](#konfiguracja-skrót)
6. [Uprawnienia](#uprawnienia)
7. [Budowanie z kodu](#budowanie-z-kodu)

---

## Wymagania

- **Java 17+**
- **MySQL** (serwer i baza dostępne z maszyny proxy)
- **Waterfall / BungeeCord** *lub* **Velocity 3.x**
- Skonfigurowane serwery **auth (limbo)** oraz **lobby** w `config.yml` / `velocity.toml` — nazwy muszą się zgadzać z `servers` w `config.yml` pluginu

---

## Instalacja (świeża)

1. Wybierz właściwy **JAR** (patrz [Pliki JAR](#pliki-jar)).
2. Wrzuć go do folderu `plugins` proxy.
3. Uruchom proxy raz, żeby powstał `plugins/UltimateBungee-Auth/config.yml` (Bungee) lub folder pluginu z `config.yml` (Velocity).
4. Uzupełnij **database** i **servers** (Auth + lobby).
5. Zrestartuj proxy.

Domyślny plik konfiguracji jest po angielsku (komentarze i struktura kluczy).

---

## Aktualizacja z wersji 1.x

Poniżej **konkretne kroki** przy przejściu ze starego UltimateBungeeAuth (np. 1.2.x, jeden JAR na Bungee) na **2.x**.

### 1. Kopia zapasowa

- Zrób backup **bazy MySQL** (tabela `authusers` lub jak masz w DB).
- Skopiuj obecny **`config.yml`** pluginu (żeby odtworzyć kolory wiadomości, nazwy serwerów, itd.).

### 2. Wybierz właściwy artefakt

| Stary plugin | Nowy plik |
|--------------|-----------|
| Jeden JAR na BungeeCord / Waterfall | **`UltimateBungeeAuth-Bungee-*.jar`** |
| Velocity (wcześniej nieobsługiwany) | **`UltimateBungeeAuth-Velocity-*.jar`** |

Nie wgrywaj obu naraz na tę samą sieć jako dwa pełne authy bez przemyślenia — jeden typ proxy = jeden JAR.

### 3. Usuń stary JAR, wgraj nowy

Usuń stary plik pluginu z `plugins`, wgraj wybrany JAR z builda `2.x`.  
**Główna klasa Bungee zmieniła się** — w `bungee.yml` w środku JAR jest już `pl.jms.auth.bungee.UltimateBungeeAuthPlugin` (nie trzeba nic edytować ręcznie).

### 4. Konfiguracja — nowa struktura (`config.yml`)

Stary `config.yml` **nie jest w 100% kompatybilny** linia-w-linię. Najbezpieczniej:

1. Pozwól pluginowi wygenerować **nowy** `config.yml` (po pierwszym starcie).
2. **Ręcznie przenieś** swoje wartości ze starego pliku według tabeli:

| Stary klucz (1.x) | Nowy klucz (2.x) |
|-------------------|------------------|
| `settings.authServer` | `servers.authServers` (lista) *lub* nadal `servers.authServer` (jedna wartość) |
| `settings.broughtServerName` | `servers.lobbyServers` *lub* `servers.lobbyServer` |
| `settings.titlesCommand` | `titlesCommandAliases` (root pliku) |
| `settings.passwordMinLength` | `passwordRules.minLength` |
| `settings.passwordMinNumbers` | `passwordRules.minUniqueDigitCount` |
| `settings.passwordMinSpecialCharacters` | `passwordRules.minSpecialCharacters` |
| `titles.settings.fadeIn` (sekundy) | `titles.settings.fadeInSeconds` |
| `titles.settings.stayIn` | `titles.settings.staySeconds` |
| `titles.settings.fadeOut` | `titles.settings.fadeOutSeconds` |
| Brak | `servers.lobbyPickMode` — `FIRST_AVAILABLE`, `RANDOM`, `ROUND_ROBIN` |
| Brak | `queue.tickSeconds` (kolejka; wcześniej było wbudowane 5 s) |
| Brak | `security.*` — limity prób, lista dozwolonych komend przed logowaniem, kanały plugin message |

Sekcja **`messages`**: klucze są w większości zbliżone, ale **sprawdź nazwy** w nowym domyślnym pliku (np. `register` → `registerPrompt`, komunikaty throttle/lockout).

Sekcja **`webhooks`**: pod **`integrations.webhooks`** (zobacz domyślny `config.yml`).

### 5. Baza danych

- Tabela jest **migrowana automatycznie** przy starcie (nowe kolumny m.in. `password_hash`, `name_lower`, indeks UUID).
- **Hasła:** stare wpisy w plaintext są przy **pierwszym poprawnym logowaniu** zamieniane na **BCrypt** i pole legacy czyszczone.
- **Zalecenie:** przed aktualizacją backup DB; po pierwszym restarcie sprawdź logi proxy pod kątem błędów SQL.

### 6. Uprawnienia — zmiana nazw

Musisz zaktualizować **LuckPerms** (lub inny plugin uprawnień):

| Stare (1.x) | Nowe (2.x) |
|-------------|------------|
| Brak / dowolny mógł używać `/uba` | `ultimatebungeeauth.admin` — **wymagane** do `/uba` |
| `uba.queue.bypass` | `ultimatebungeeauth.queue.bypass` |

Jeśli nie nadasz `ultimatebungeeauth.admin`, tylko uprawnieni moderatorzy będą mogli resetować konta z `/uba`.

### 7. Zachowanie przy logowaniu

- **Złe hasło:** gracz dostaje komunikat (jak w `messages`), a niekoniecznie natychmiastowy kick — dostosuj `security.maxPasswordAttempts` i `lockoutSeconds`.
- **Kolejka:** działa jako FIFO po wielu serwerach auth — ustaw **`servers.authServers`** jeśli masz kilka limbo.

### 8. Velocity

Jeśli przesiadasz się z samego Bungee na **Velocity**, użyj JAR-a Velocity, skonfiguruj **ten sam MySQL** i **odtwórz** te same wartości w `config.yml` pluginu (Velocity ma osobny katalog danych pluginu).

---

## Pliki JAR

Build Gradle (`./gradlew build` lub `gradlew.bat build`):

| Moduł | Artefakt (przykładowa nazwa) |
|-------|------------------------------|
| Bungee | `bungee/build/libs/UltimateBungeeAuth-Bungee-2.x.x.jar` |
| Velocity | `velocity/build/libs/UltimateBungeeAuth-Velocity-2.x.x.jar` |

JAR zawiera wbudowany moduł **`core`** (nie trzeba dokładać osobnego pliku).

---

## Konfiguracja (skrót)

- **`database`** — połączenie MySQL i nazwa tabeli (`table`, domyślnie `authusers`).
- **`servers`** — `authServers` / `lobbyServers` (listy), `lobbyPickMode`.
- **`passwordRules`** — siła hasła przy rejestracji.
- **`queue.tickSeconds`** — jak często przetwarzana jest kolejka na serwerach auth.
- **`security`** — uprawnienia nazwane, throttle IP, whitelist komend przed logowaniem, kanały `BungeeCord` / `Return` pod integrację z backendem.
- **`messages`**, **`titles`**, **`integrations.webhooks`**.

Pełny szablon: `bungee/src/main/resources/config.yml` oraz `velocity/src/main/resources/config.yml`.

---

## Uprawnienia

- `ultimatebungeeauth.admin` — komendy `/uba` (administracja kontami).
- `ultimatebungeeauth.queue.bypass` — pominięcie kolejki na lobby po zalogowaniu.

---

## Budowanie z kodu

Wymagania: **JDK 17**, opcjonalnie Gradle Wrapper z repozytorium.

```bash
./gradlew build
```

Na Windows: `gradlew.bat build`

---

## Krótki opis pod stronę pobrania (Spigot itd.)

Możesz wkleić poniższy blok jako opis zasobu (edytuj listę funkcji wg potrzeby):

---

**UltimateBungeeAuth 2.x** — logowanie na proxy z **MySQL**, obsługa **Waterfall/BungeeCord** oraz **Velocity 3**, kolejka do lobby, tryb premium (online), limity prób logowania, **BCrypt**, integracja z backendem przez plugin messaging i opcjonalne webhooks.

**Wymaga Java 17.** Przy aktualizacji z 1.x: nowa struktura `config.yml`, nowe uprawnienia (`ultimatebungeeauth.*`), migracja tabeli i haseł przy pierwszym logowaniu — zobacz **README / sekcja „Aktualizacja”** w repozytorium.

---

Wersja w pliku [`version.txt`](version.txt) odpowiada wersji projektu Gradle.
