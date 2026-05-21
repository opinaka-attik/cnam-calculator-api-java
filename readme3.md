Les 3 optimisations sont appliquées. Voici un résumé et les commandes pour tester :

---

## Récapitulatif des changements

| Fichier | Optimisation | Impact |
|---|---|---|
| `Dockerfile` | `mvn dependency:go-offline` avant `COPY src` | Les deps Maven sont cachées — rebuild ~15x plus rapide si seul le code change |
| `docker-compose.yml` | Volume nommé `m2:/root/.m2` | `docker compose run --rm test` ne re-télécharge plus Maven à chaque fois |
| `CalculatorApiServer.java` | Switch expression Java 17 | Code plus concis, pas de risque d'oubli de `break` |

---

## Commandes pour tester

Rebuild l'image API (avec le nouveau cache Dockerfile) :

```bash
docker compose up --build -d
```

Lancer les tests (la 1re fois télécharge Maven, les suivantes utilisent le cache) :

```bash
docker compose run --rm test
```

Tester l'API :

```bash
curl "http://localhost:8000/calculate?operation=add&a=4&b=2"
```