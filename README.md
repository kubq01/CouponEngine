# Coupon Engine

**English version below**

## Opis aplikacji

Aplikacja umożliwia:

- generowanie kuponów,
- wykorzystanie kuponów przez użytkowników,
- ograniczenie liczby użyć kuponu,
- walidację kraju użytkownika na podstawie adresu IP,
- zabezpieczenie przed wielokrotnym użyciem kuponu przez tego samego użytkownika

Każdy kupon:

- posiada unikalny identyfikator (`case-insensitive`),
- może zostać użyty maksymalnie określoną liczbę razy,
- może zostać użyty tylko raz przez danego użytkownika,
- jest przypisany do konkretnego kraju.

---

# Uruchomienie aplikacji

## Wymagania

- Docker
- Java
- Maven

## Budowanie aplikacji

```bash
mvn clean package
```

## Uruchomienie aplikacji

```bash
docker compose up
```

Aplikacja zostanie uruchomiona wraz ze wszystkimi wymaganymi zależnościami.

---

# Uruchamianie testów

```bash
mvn test
```

Do uruchamiania testów wymagany jest Docker, ponieważ wykorzystywane są kontenery Testcontainers.

---

# Decyzje techniczne

## Walidacja danych wejściowych

Wszystkie tekstowe dane wejściowe są walidowane przed wykonaniem jakichkolwiek operacji biznesowych lub zapytań do bazy danych.

Walidacja obejmuje:

- ograniczenie długości danych,
- dopuszczenie wyłącznie znaków alfanumerycznych

Celem tego podejścia jest:

- zmniejszenie ryzyka błędów aplikacyjnych,
- ograniczenie możliwości wykonania ataków typu SQL Injection

---

## Dane lokalizacyjne użytkownika

Kraj użytkownika określany jest na podstawie adresu IP pochodzącego z requestu HTTP.

Obsługiwane jest również `X-Forwarded-For`, co umożliwia poprawne działanie aplikacji za:

- reverse proxy,
- load balancerem,
- API Gateway,
- CDN.

Bez tego aplikacja identyfikowałaby adres IP proxy zamiast rzeczywistego adresu użytkownika.

Do mapowania adresu IP na kraj wykorzystano bazę danych GeoLite.

Rozwiązanie to zostało wybrane ponieważ:

- jest bardzo popularne i sprawdzone produkcyjnie,
- działa lokalnie bez potrzeby wykonywania zewnętrznych requestów HTTP,
- zapewnia bardzo niskie opóźnienia,
- eliminuje zależność od zewnętrznych API,
- pozwala uniknąć limitów requestów oraz problemów z dostępnością usług zewnętrznych.

Lokalna baza GeoLite jest również bardziej przewidywalna pod względem wydajności niż rozwiązania oparte o zewnętrzne API geolokalizacyjne.

---

## Eliminowanie zduplikowanych kuponów

Nazwy kuponów są normalizowane do formatu `lowercase` przed zapisaniem do bazy danych.

Dzięki temu:

```text
SUMMER2025
summer2025
Summer2025
```

są traktowane jako ten sam kupon.

Przed zapisaniem wykonywane jest sprawdzenie:

```java
couponRepository.existsById(coupon.getId().toLowerCase())
```

Takie rozwiązanie:

- upraszcza logikę wyszukiwania,
- eliminuje problemy związane z wielkością liter,
- zapewnia spójność danych,
- pozwala uniknąć trudnych do wykrycia duplikatów.

---

## Unikanie race condition przy inkrementacji użyć kuponu

Do inkrementacji liczby użyć kuponu wykorzystano atomowe zapytanie SQL:

```java
UPDATE CouponEntity c
SET c.currentUsages = c.currentUsages + 1
WHERE c.id = :id AND c.currentUsages < c.maxUsages
```

Zapytanie zwraca liczbę zmodyfikowanych rekordów.

Jeżeli:

```java
updated != 1
```

oznacza to, że limit użyć został osiągnięty.

To podejście eliminuje race condition bez konieczności używania:

- pessimistic lockingu,
- optimistic lockingu,
- synchronizacji po stronie aplikacji.

Pessimistic locking:

- blokowałby rekord w bazie danych,
- zmniejszałby throughput aplikacji,
- zwiększałby ryzyko deadlocków,
- gorzej skalowałby się przy dużym ruchu.

Optimistic locking:

- wymagałby retry mechanizmu,
- powodowałby dodatkowe roundtripy do bazy,
- zwiększałby złożoność kodu.

Atomic SQL update:

- jest prostszy,
- bardziej deterministyczny,
- wykorzystuje natywne mechanizmy bazy danych
- wykonuje pojedyncze zapytanie SQL,
- nie wymaga utrzymywania locka,
- działa wydajniej pod dużym obciążeniem.

---

## Unikanie race condition przy oznaczaniu użycia kuponu przez użytkownika

Tabela `coupon_usage` posiada unikalny constraint:

```text
(coupon_id, user_id)
```

Dodatkowo zastosowano natywne zapytanie SQL:

```sql
INSERT INTO coupon_usage (id, coupon_id, user_id)
VALUES (...)
ON CONFLICT (coupon_id, user_id) DO NOTHING
```

Rozwiązanie pozwala uniknąć problemu:

```text
SELECT -> INSERT
```

który jest podatny na race condition.

- operacja jest atomowa,
- nie wymaga wcześniejszego sprawdzania istnienia rekordu,
- nie powoduje wyjątków przy konflikcie,
- upraszcza obsługę błędów,
- poprawia wydajność przy dużej liczbie równoczesnych requestów.

Dzięki temu aplikacja może zwrócić kontrolowany status:

```java
COUPON_ALREADY_USED_BY_USER_FAILURE
```

zamiast błędu bazy danych.

---

## Kolejność operacji biznesowych

Najpierw zapisywane jest użycie kuponu przez użytkownika:

```java
insertIfNotExists(...)
```

a dopiero później inkrementowany jest licznik użyć kuponu:

```java
incrementIfPossible(...)
```

Może wystąpić sytuacja, w której:

- użytkownik zostanie zapisany jako używający kuponu,
- ale limit użyć kuponu został już osiągnięty.

W takim przypadku wpis użytkownika nie jest usuwany ponieważ kupon i tak nie może zostać już wykorzystany przez żadnego użytkownika.

Usuwanie takiego wpisu:

- zwiększałoby złożoność transakcji,
- zwiększałoby ryzyko kolejnych race condition.

To podejście upraszcza logikę systemu i zachowuje poprawność biznesową.

---

## Transakcyjność operacji

Metoda:

```java
@Transactional
public UseCouponResponse useCoupon(...)
```

wykonuje wszystkie operacje w ramach jednej transakcji bazodanowej.

Zapewnia to:

- spójność danych,
- atomowość operacji,
- poprawne zachowanie w środowisku współbieżnym.

---

## Skalowalność rozwiązania

Aplikacja została zaprojektowana z myślą o środowisku wielowątkowym i wysokiej współbieżności.

Najważniejsze elementy wspierające skalowalność:

- atomowe operacje SQL,
- brak locków aplikacyjnych,
- ograniczenie liczby roundtripów do bazy danych,
- wykorzystanie constraintów bazy danych zamiast synchronizacji po stronie aplikacji,
- stateless service architecture.

---

## Testowanie

Do testów integracyjnych wykorzystano Testcontainers.

Dzięki temu testy uruchamiane są na rzeczywistej bazie danych w kontenerze Docker.

- środowisko testowe jest zbliżone do produkcyjnego,
- możliwe jest poprawne testowanie współbieżności,
- możliwe jest testowanie natywnych funkcji SQL,
- eliminowane są różnice pomiędzy bazą in-memory a rzeczywistą bazą danych.

Bez użycia Testcontainers nie byłoby pewności, że rozwiązania eliminujące race condition działają poprawnie w realnym środowisku.

---

## Konteneryzacja

Aplikacja jest w pełni konteneryzowana przy użyciu Docker Compose.

- brak konieczności ręcznej instalacji zależności,
- powtarzalne środowisko uruchomieniowe,
- łatwiejszy deployment,
- prostsze uruchamianie projektu przez innych developerów,
- większa zgodność środowisk development/test/production.

---

## Rozdzielenie logiki biznesowej od warstwy persystencji

Logika biznesowa znajduje się w `CouponService`, natomiast operacje bazodanowe w repository.

Takie podejście:

- zwiększa czytelność projektu,
- ułatwia testowanie,
- upraszcza rozwój aplikacji,
- pozwala niezależnie rozwijać warstwy aplikacji.

---
# English Version

## Application Description

The application allows:

- generating coupons,
- redeeming coupons by users,
- limiting coupon usage count,
- validating the user country based on IP address,
- preventing multiple usages of the same coupon by the same user.

Each coupon:

- has a unique identifier (`case-insensitive`),
- can be used only a limited number of times,
- can be used only once per user,
- is assigned to a specific country.

---

# Running the Application

## Requirements

- Docker
- Java
- Maven

## Building the Application

```bash
mvn clean package
```

## Starting the Application

```bash
docker compose up
```

The application will start together with all required dependencies.

---

# Running Tests

```bash
mvn test
```

Docker is required for running tests because the project uses Testcontainers.

---

# Technical Decisions

## Input Validation

All textual input data is validated before any business logic or database operations are executed.

Validation includes:

- maximum length restrictions,
- allowing only alphanumeric characters.

The purpose of this approach is:

- reducing the risk of application errors,
- reducing the possibility of SQL Injection attacks.

---

## User Geolocation Data

The user country is determined based on the IP address from the HTTP request.

The application also supports the `X-Forwarded-For` header, which allows correct behavior behind:

- reverse proxies,
- load balancers,
- API gateways,
- CDNs.

Without this, the application would identify the proxy IP address instead of the actual user IP address.

The GeoLite database is used for IP-to-country mapping.

This solution was selected because:

- it is widely used and production-proven,
- it works locally without external HTTP requests,
- it provides very low latency,
- it removes dependency on external APIs,
- it avoids rate limits and availability issues of third-party services.

A local GeoLite database is also more predictable in terms of performance compared to external geolocation APIs.

---

## Preventing Duplicate Coupons

Coupon identifiers are normalized to `lowercase` before being stored in the database.

Because of this:

```text
SUMMER2025
summer2025
Summer2025
```

are treated as the same coupon.

Before saving, the application performs the following check:

```java
couponRepository.existsById(coupon.getId().toLowerCase())
```

This approach:

- simplifies lookup logic,
- eliminates case-sensitivity issues,
- ensures data consistency,
- prevents hard-to-detect duplicates.

---

## Preventing Race Conditions During Coupon Usage Increment

Coupon usage count is incremented using an atomic SQL query:

```java
UPDATE CouponEntity c
SET c.currentUsages = c.currentUsages + 1
WHERE c.id = :id AND c.currentUsages < c.maxUsages
```

The query returns the number of modified rows.

If:

```java
updated != 1
```

it means that the maximum usage limit has been reached.

This approach eliminates race conditions without requiring:

- pessimistic locking,
- optimistic locking,
- application-level synchronization.

Pessimistic locking:

- would lock database rows,
- would reduce application throughput,
- would increase deadlock risk,
- would scale worse under high traffic.

Optimistic locking:

- would require retry mechanisms,
- would introduce additional database roundtrips,
- would increase code complexity.

Atomic SQL update:

- is simpler,
- is more deterministic,
- uses native database mechanisms,
- executes a single SQL query,
- does not require maintaining locks,
- performs better under high load.

---

## Preventing Race Conditions During User Coupon Usage Registration

The `coupon_usage` table contains a unique constraint:

```text
(coupon_id, user_id)
```

Additionally, a native SQL query is used:

```sql
INSERT INTO coupon_usage (id, coupon_id, user_id)
VALUES (...)
ON CONFLICT (coupon_id, user_id) DO NOTHING
```

This solution avoids the problem of:

```text
SELECT -> INSERT
```

which is vulnerable to race conditions.

Benefits of this approach:

- the operation is atomic,
- it does not require checking record existence beforehand,
- it avoids throwing exceptions on conflicts,
- it simplifies error handling,
- it improves performance under high concurrency.

Because of this, the application can return a controlled status:

```java
COUPON_ALREADY_USED_BY_USER_FAILURE
```

instead of a database exception.

---

## Order of Business Operations

First, the coupon usage by the user is stored:

```java
insertIfNotExists(...)
```

and only afterwards the coupon usage counter is incremented:

```java
incrementIfPossible(...)
```

There is a possible scenario where:

- the user is marked as having used the coupon,
- but the maximum coupon usage limit has already been reached.

In this case, the user usage entry is not removed because the coupon can no longer be used by any user anyway.

Removing such entry:

- would increase transaction complexity,
- would increase the risk of additional race conditions.

This approach simplifies the system logic while maintaining business correctness.

---

## Transaction Management

The method:

```java
@Transactional
public UseCouponResponse useCoupon(...)
```

executes all operations within a single database transaction.

This guarantees:

- data consistency,
- operation atomicity,
- correct behavior in concurrent environments.

---

## Scalability

The application was designed for high concurrency and multi-threaded environments.

The most important scalability-related elements are:

- atomic SQL operations,
- no application-level locks,
- minimizing database roundtrips,
- using database constraints instead of application synchronization,
- stateless service architecture.

---

## Testing

Integration tests use Testcontainers.

Because of this, tests run against a real database instance inside Docker containers.

Benefits of this approach:

- the test environment is close to production,
- proper concurrency testing is possible,
- native SQL features can be tested,
- differences between in-memory databases and real databases are eliminated.

Without Testcontainers, there would be no guarantee that race condition prevention mechanisms work correctly in a real environment.

---

## Containerization

The application is fully containerized using Docker Compose.

Benefits:

- no manual dependency installation required,
- reproducible runtime environment,
- easier deployment,
- easier project setup for other developers,
- better consistency between development, test, and production environments.

---

## Separation of Business Logic and Persistence Layer

Business logic is located in `CouponService`, while database operations are handled by repositories.

This approach:

- improves project readability,
- simplifies testing,
- makes application development easier,
- allows independent development of application layers.
