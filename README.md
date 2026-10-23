# Note Vault

Note Vault is a REST API for registering users and creating private, JWT-protected notes. Each user's note listing and note operations are scoped to that user.

## Run locally

### Docker Compose

With Docker and the Compose plugin installed, start the API and PostgreSQL:

```sh
docker compose up --build
```

The API listens at `http://localhost:8090`. Stop the services with `Ctrl+C`; run `docker compose down` to remove the containers.

### Run the app with Maven

Start a PostgreSQL instance and create a database named `note_vault`, then run:

```sh
./mvnw spring-boot:run
```

By default the app connects to `jdbc:postgresql://localhost:5432/note_vault` with username and password `postgres`. Override the connection settings with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. The app listens on port `8090`.

## API walkthrough

The following example requires `curl` and `jq`. It registers a user, logs in, creates a note, and lists that user's notes:

```sh
BASE_URL=http://localhost:8090

# Register (returns the created user's id and username).
curl -i -X POST "$BASE_URL/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"change-me"}'

# Log in and save the access token for authenticated requests.
TOKEN=$(curl -sS -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"change-me"}' | jq -r '.accessToken')

# Create a note (returns 201 Created and the note).
curl -i -X POST "$BASE_URL/api/notes" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"First note","content":"A private note","tags":["personal","demo"]}'

# List notes for the authenticated user (page is zero-based).
curl -i "$BASE_URL/api/notes?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

Registration and login return `200 OK`. Include the access token from login in the authorization header for note requests. Listing requires the `page` and `size` query parameters; optional `q` and `tag` parameters filter the results.

## HTTP status codes

- **401 Unauthorized**: Authentication is missing or invalid for a protected endpoint, or login credentials are invalid. A locked-out login also returns 401. Obtain a valid access token by logging in.
- **403 Forbidden**: The caller is authenticated but an operation is explicitly forbidden. The API has no role-based access restrictions; note access is instead scoped to the owner. The current exception handler maps `UserLockException` to 403, while the login lockout flow currently returns 401.
- **404 Not Found**: A note does not exist, has been deleted, or belongs to another user. Returning the same status for inaccessible and nonexistent notes avoids revealing whether another user's note exists.

## Note endpoints

- `POST /api/notes`: Create a note.
- `GET /api/notes?page=0&size=20`: List the current user's notes.
- `GET /api/notes/{id}`: Get one of the current user's notes.
- `PUT /api/notes/{id}`: Update one of the current user's notes.
- `DELETE /api/notes/{id}`: Soft-delete one of the current user's notes.
