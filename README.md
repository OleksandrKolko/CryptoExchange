# Біржа криптовалют

Android-застосунок, який показує **топ-30 криптовалют за ринковою капіталізацією** з цінами в USD. Дані зберігаються в **Room**, оновлюються у фоні через **WorkManager** і доступні **без Інтернету** після першого завантаження.

🎥 **Демонстраційне відео:** _посилання_

## Команда

| Учасник | Відповідальність |
| --- | --- |
| Колко Олександр | `domain` (модель, контракт репозиторію, use case'и), `presentation` (ViewModel, Compose UI) |
| Сугоняк Роман | `data` (API, DTO, Room, репозиторій), `worker` (WorkManager), unit-тести |

## Технології

Kotlin 2.4.10 · Jetpack Compose · MVVM · Retrofit · Room 3 · WorkManager · Coil 3

## Архітектура

Три шари в одному модулі `app`:

| Пакет | Що містить |
| --- | --- |
| `domain` | Модель `Coin`, інтерфейс `CryptoRepository`, сценарії (use case'и). Не залежить від Android, Room чи Retrofit. |
| `data` | API (Retrofit + DTO), база Room (Entity, DAO), мапери, реалізація репозиторію. |
| `presentation` | `CryptoViewModel`, стан екрана, Compose-екрани. |
| `worker`, `di` | Фонова синхронізація (WorkManager) та ручне впровадження залежностей. |

```text
WorkManager → Worker → репозиторій → CoinGecko API → Room
Room → Flow → репозиторій → ViewModel → StateFlow → Compose
```

Список на екрані **завжди читається з Room**. Оновлення рейтингу виконується в одній транзакції; при помилці збережені дані не змінюються.

## API

**CoinGecko Demo API** — `/coins/markets`

```text
GET https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=30&page=1
x-cg-demo-api-key: <ключ>
```

CoinGecko використано замість CoinDesk Data API, бо CoinDesk більше не видає безкоштовні ключі. Заміна торкнулася лише пакета `data`.

## Фонова синхронізація

* **Періодична:** раз на **1 годину**, `crypto_sync_periodic`, політика `KEEP` (без дублікатів).
* **Ручна:** кнопка «Оновити», `crypto_sync_manual`, політика `KEEP`.
* Обидві виконуються лише за наявності мережі; тимчасові помилки повторюються з backoff, помилки ключа — ні.

## Запуск

1. Отримайте безкоштовний Demo-ключ на [coingecko.com/en/api](https://www.coingecko.com/en/api).
2. Додайте його у файл `local.properties` (він не потрапляє в Git):

   ```properties
   COINGECKO_API_KEY=
   ```

3. Відкрийте проєкт в Android Studio, виконайте **Sync Project with Gradle Files** і запустіть `app`.

## Тести

```bash
./gradlew testDebugUnitTest
```

Unit-тести репозиторію перевіряють: відсутність дублікатів, заміну рейтингу, збереження кешу при помилці, обробку відсутніх цін.

## Додатково ⭐

* Пошук за назвою чи символом серед збережених валют.
* Екран деталей валюти; у горизонтальному режимі — список і деталі поруч.
