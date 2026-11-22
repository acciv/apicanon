# apicanon

Канонизатор API-запросов на чистом Java. Приводит запрос к стабильному виду,
считает отпечаток SHA-256 и показывает структурную разницу между двумя запросами.

Это анализ формата и структуры. Никакого подбора ключей, взлома и обхода
авторизации. Значение `Authorization` в отпечатке заменяется на `Bearer ***`.

## Что делает

- нормализует путь: удаляет дубли слешей, `.` и `..`, декодирует `%XX`;
- сортирует query-параметры и повторяющиеся значения;
- приводит имена заголовков к нижнему регистру;
- выбрасывает летучие заголовки вроде `X-Request-Id` и `Date`;
- сортирует ключи JSON-тела;
- считает стабильный отпечаток `sha256:...`;
- строит diff по полям `method`, `path`, `query`, `headers`, `body`.

## Сборка и тесты

Нужна JDK 17. Внешних библиотек нет.

```bash
javac -d out/tools tools/Build.java
java -cp out/tools Build
```

Build.java сам компилирует `src/main/java` и `src/test/java`, затем запускает
тесты через `dev.acciv.apicanon.TestRunner`.

## CLI

```bash
java -cp out/classes dev.acciv.apicanon.Cli canon examples/request-a.json
java -cp out/classes dev.acciv.apicanon.Cli diff examples/request-a.json examples/request-b.json
```

## Формат входного файла

```json
{
  "method": "post",
  "path": "/v1//users/42?b=2&a=1",
  "headers": {
    "Content-Type": "application/json",
    "Authorization": "Bearer ghp_FAKE_0001"
  },
  "body": {"z": 1, "a": 2}
}
```

Query можно писать прямо в `path` или отдельным полем `query`. Повторяющиеся
значения допустимы.

## Пример вывода diff

```json
{
  "left_fingerprint": "sha256:...",
  "right_fingerprint": "sha256:...",
  "changes": [
    {"path": "$.query[2][1]", "type": "added", "before": "<missing>", "after": "hello world"}
  ]
}
```
