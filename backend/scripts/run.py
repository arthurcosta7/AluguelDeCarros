#!/usr/bin/env python3
import os
from pathlib import Path
import re
import sys
from urllib.parse import urlparse


def configuration(directory, environment):
    values = dict(environment)
    path = directory / '.env'
    if path.exists():
        for line in path.read_text().splitlines():
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            if line.startswith('export '):
                line = line[7:]
            key, separator, value = line.partition('=')
            key, value = key.strip(), value.strip()
            if not separator or not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', key):
                raise ValueError('Linha inválida no .env; use NOME=valor.')
            if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
                value = value[1:-1]
            values.setdefault(key, value)

    if not values.get('DB_URL'):
        host = urlparse(values.get('VITE_SUPABASE_URL', '')).hostname or ''
        if not re.fullmatch(r'[a-z0-9]+\.supabase\.co', host):
            raise ValueError('Configure DB_URL ou uma VITE_SUPABASE_URL válida no backend/.env.')
        project = host.split('.')[0]
        values['DB_URL'] = f'jdbc:postgresql://db.{project}.supabase.co:5432/postgres?sslmode=require&connectTimeout=10'
        username = values.get('DB_USERNAME', 'postgres')
        values['DB_USERNAME'] = username.removesuffix(f'.{project}')
        print('Supabase: conexão direta com TLS. Para redes IPv4, configure DB_URL com o Session pooler.', flush=True)
    if not values['DB_URL'].startswith('jdbc:postgresql://'):
        raise ValueError('DB_URL deve usar o formato jdbc:postgresql://host:5432/postgres?sslmode=require.')
    if not values.get('DB_USERNAME') or not values.get('DB_PASSWORD'):
        raise ValueError('Preencha DB_USERNAME e DB_PASSWORD no backend/.env.')
    return values


def main():
    directory = Path(__file__).resolve().parents[1]
    try:
        values = configuration(directory, os.environ)
    except ValueError as error:
        sys.exit(str(error))
    os.chdir(directory)
    os.execvpe('./mvnw', ['./mvnw', 'spring-boot:run', *sys.argv[1:]], values)


if __name__ == '__main__':
    main()
