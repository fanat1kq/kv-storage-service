# kv-storage-service

gRPC-сервис на Java для работы с key-value хранилищем, используя Tarantool


## 🚀 Основные возможности

- **put(key, value)** - сохраняет в БД значение для новых ключей и перезаписывает значение для существующих,
- **get(key)** - возвращает значение для указанного ключа,
- **delete(key)** - удаляет значение для указанного ключа,
- **range(key_since, key_to)** → отдает gRPC stream пар ключ-значение из запрошенного диапазона,
- **count()** - возвращает количествово записей в БД.

## 🏗️ Архитектура

### Backend
- **Java 17** + **Spring Boot 4**
- **Tarantool** для kv хранения
- **TestContainers** для тестирования приложения с поднятия инстанса БД Tarantool

## ⚙️ Настройка и запуск



1. **Клонируйте репозиторий:**
```bash
git clone https://github.com/fanat1kq/kv-storage-service.git
cd kv-storage-service
```

2. **Запустите инфраструктуру:**
```bash
1 Заполнить .env в соответствии с вашими желаемым профилем по примеру env.example
2 
 -  Если своя БД через идею
 -  Если через docker
docker-compose up -d
```


Приложение будет доступно по адресу: `http://{HOST}`

## 📝 API Endpoints


### Управление данными
# 1. Put (сохранить значение)
```bash
grpcurl -plaintext -d '{"key":"test","value":"SGVsbG8="}' localhost:8080 kvservice.KVService/Put
```

# 2. Get (получить значение)
```bash
grpcurl -plaintext -d '{"key":"test"}' localhost:8080 kvservice.KVService/Get
```

# 3. Count (количество записей)
```bash
grpcurl -plaintext -d '{}' localhost:8080 kvservice.KVService/Count
```

# 4. Range (диапазон)
```bash
grpcurl -plaintext -d '{"keySince":"a","keyTo":"z"}' localhost:8080 kvservice.KVService/Range
```

# 5. Delete (удалить)
```bash
grpcurl -plaintext -d '{"key":"test"}' localhost:8080 kvservice.KVService/Delete
```
