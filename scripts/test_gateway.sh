#!/bin/bash

# ==============================================================================
# Script per testare la configurazione di Spring Cloud API Gateway
# Prerequisiti: curl e jq devono essere installati.
# Esempio di installazione:
#   - su Debian/Ubuntu: sudo apt-get install curl jq
#   - su macOS: brew install curl jq
# ==============================================================================

# --- Configurazione ---
GATEWAY_URL="http://localhost:8080"
TOKEN_USER=""
TOKEN_ADMIN=""

# --- Colori per l'output ---
COLOR_RESET='\033[0m'
COLOR_INFO='\033[0;36m'
COLOR_SUCCESS='\033[0;32m'
COLOR_ERROR='\033[0;31m'
COLOR_SECTION='\033[1;33m'

# --- Funzioni Helper ---
print_section() {
    echo -e "\n${COLOR_SECTION}### $1 ###${COLOR_RESET}"
}

print_info() {
    echo -e "${COLOR_INFO}$1${COLOR_RESET}"
}

# Funzione per eseguire un test e verificare il codice di stato HTTP
# Argomenti:
# 1. Descrizione del test
# 2. Codice/i di stato HTTP attesi (es. "200" o "401|403")
# 3. Comando curl completo come array
run_test() {
    local description="$1"
    local expected_codes="$2"
    shift 2
    local curl_command=("$@")

    # Stampa la descrizione del test in modo allineato
    printf "  -> Test: %-65s" "$description"

    # Esegui curl, catturando solo il codice di stato HTTP
    actual_code=$("${curl_command[@]}" -s -o /dev/null -w "%{http_code}")

    # Verifica se il codice ottenuto è tra quelli attesi
    if [[ "$expected_codes" == *"$actual_code"* ]]; then
        echo -e "${COLOR_SUCCESS}[SUCCESS]${COLOR_RESET} (Expected: ${expected_codes}, Got: ${actual_code})"
    else
        echo -e "${COLOR_ERROR}[FAILED]${COLOR_RESET}  (Expected: ${expected_codes}, Got: ${actual_code})"
    fi
}


# ==============================================================================
# Inizio dell'esecuzione dei test
# ==============================================================================

print_info "========================================="
print_info "  API Gateway Test Script"
print_info "  Target: ${GATEWAY_URL}"
print_info "========================================="


# --- Sezione 1: Autenticazione e Generazione Token ---
print_section "Sezione 1: Authentication Service (/auth/**)"

# 1.1: Login utente 'user'
print_info "  -> Tentativo di login per 'user' per ottenere il token..."
TOKEN_USER=$(curl -s -X POST "${GATEWAY_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "password"}' | jq -r '.token')

if [[ -z "$TOKEN_USER" || "$TOKEN_USER" == "null" ]]; then
    echo -e "${COLOR_ERROR}  [ERRORE CRITICO] Impossibile ottenere il token per 'user'. Lo script non può continuare.${COLOR_RESET}"
    exit 1
else
    echo -e "${COLOR_SUCCESS}  [OK] Token per 'user' ottenuto.${COLOR_RESET}"
fi

# 1.2: Login utente 'admin'
print_info "  -> Tentativo di login per 'admin' per ottenere il token..."
TOKEN_ADMIN=$(curl -s -X POST "${GATEWAY_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}' | jq -r '.token')

if [[ -z "$TOKEN_ADMIN" || "$TOKEN_ADMIN" == "null" ]]; then
    echo -e "${COLOR_ERROR}  [ERRORE CRITICO] Impossibile ottenere il token per 'admin'. Lo script non può continuare.${COLOR_RESET}"
    exit 1
else
    echo -e "${COLOR_SUCCESS}  [OK] Token per 'admin' ottenuto.${COLOR_RESET}"
fi

# 1.3: Login fallito
run_test "Login con password errata" "500" \
    curl -X POST "${GATEWAY_URL}/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username": "user", "password": "wrongpassword"}'


# --- Sezione 2: Rate Limiter ---
print_section "Sezione 2: Test del Rate Limiter (su /auth/**)"
print_info "  -> Invio di 15 richieste in rapida successione. Le prime dovrebbero essere 200, le successive 429."
for i in {1..15}; do
   code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${GATEWAY_URL}/auth/login" \
     -H "Content-Type: application/json" -d '{"username": "user", "password": "password"}')
   
   if [[ "$code" == "429" ]]; then
       echo -e "  Richiesta ${i}: Codice ${COLOR_ERROR}${code}${COLOR_RESET} (Rate Limit attivato)"
   else
       echo -e "  Richiesta ${i}: Codice ${COLOR_SUCCESS}${code}${COLOR_RESET}"
   fi
done


# --- Sezione 3: Product Service (Rotta Protetta) ---
# Nota: Assicurati che il tuo ProductController risponda su "/api/products/**" come definito nel gateway
print_section "Sezione 3: Product Service (/api/products/**)"

run_test "Accesso senza token" "401" \
    curl -X GET "${GATEWAY_URL}/api/products/user-profile" -H "X-Version: v2"

run_test "Accesso con token non valido" "401" \
    curl -X GET "${GATEWAY_URL}/api/products/user-profile" -H "X-Version: v2" -H "Authorization: Bearer invalid.token"

run_test "Accesso senza header X-Version" "500" \
    curl -X GET "${GATEWAY_URL}/api/products/user-profile" -H "Authorization: Bearer ${TOKEN_USER}"

run_test "Utente 'user' accede a risorsa USER" "200" \
    curl -X GET "${GATEWAY_URL}/api/products/user-profile" -H "X-Version: v2" -H "Authorization: Bearer ${TOKEN_USER}"

run_test "Utente 'user' accede a risorsa ADMIN (accesso negato)" "403" \
    curl -X GET "${GATEWAY_URL}/api/products/admin-dashboard" -H "X-Version: v2" -H "Authorization: Bearer ${TOKEN_USER}"

run_test "Utente 'admin' accede a risorsa ADMIN" "200" \
    curl -X GET "${GATEWAY_URL}/api/products/admin-dashboard" -H "X-Version: v2" -H "Authorization: Bearer ${TOKEN_ADMIN}"

run_test "Utente 'admin' accede a risorsa USER" "200" \
    curl -X GET "${GATEWAY_URL}/api/products/user-profile" -H "X-Version: v2" -H "Authorization: Bearer ${TOKEN_ADMIN}"


# --- Sezione 4: User Service (Rotta Protetta) ---
print_section "Sezione 4: User Service (/api/users/**)"

# Nota: per questo test, 200 (trovato) o 404 (non trovato) sono entrambi successi dal punto di vista del gateway.
# L'importante è che non sia un 401 (Unauthorized) o 403 (Forbidden).
run_test "Accesso con metodo consentito (GET)" "200|404" \
    curl -X GET "${GATEWAY_URL}/api/users/123" -H "Authorization: Bearer ${TOKEN_USER}"

run_test "Accesso con metodo NON consentito (DELETE)" "404" \
    curl -X DELETE "${GATEWAY_URL}/api/users/123" -H "Authorization: Bearer ${TOKEN_ADMIN}"

print_info "\n========================================="
print_info "  Test completati."
print_info "========================================="
