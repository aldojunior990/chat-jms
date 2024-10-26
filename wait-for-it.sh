#!/bin/bash
# wait-for-it.sh

# Espera até que o ActiveMQ esteja acessível
until nc -z activemq 61616; do
  echo "Aguardando ActiveMQ..."
  sleep 2
done

# Inicia a aplicação
exec "$@"