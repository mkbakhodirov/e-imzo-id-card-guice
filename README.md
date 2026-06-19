# E-IMZO ID Card Guice

Короткая инструкция для запуска проекта на Windows Local PC.

## Требования

- Установлен и запущен Docker Desktop.
- Порты `80`, `6379`, `8080`, `8081` свободны.
- На компьютере есть папка `C:\e-imzo-server` с файлами распакованные с zip файла (файл находится на телеграм канале E-IMZO Knowledge Base https://t.me/c/1649793225/59).
- Внутри `C:\e-imzo-server` должны быть `keys` и `test-config.properties`. Обращайтесь к Замире опа @baxaabdu.
- Сформировать UPLOAD URL исходя из вашего веб домена, например https://example.uz/frontend/mobile/upload (Можно воспользоваться ngrok.com для локальной разработки). Получить SiteID на этот UPLOAD URL у Замиры опа.
- На телефоне установлено приложение "E-IMZO (ID Карта)".

## Настройка

Добавьте в `C:\e-imzo-server\test-config.properties`:

```properties
# Пропишите свой SiteID
mobile.siteId=0000

mobile.storage.redis.host=127.0.0.1
mobile.storage.redis.password=test
mobile.storage.redis.db=1
```

## Что входит в проект

Проект запускается через Docker Compose и использует:

- `nginx`;
- `redis`;
- `e-imzo-server`;
- Java web app на Guice и Jetty.

## Запуск

Откройте терминал в папке текущего проекта `e-imzo-id-card-guice` и выполните:

```powershell
docker compose up --build
```

## Проверка

API:

```text
http://localhost:8080/ping
```

Должен вернуть JSON.

Рабочий веб-сайт:

```text
https://example.uz
```

## Подписание с телефона

В приложении "E-IMZO (ID Карта)" включите dev mode и добавьте тестовый ключ.

Откройте веб-сайт, отсканируйте QR код и подпишите документ в приложении.

## Остановка

```powershell
docker compose down
```
