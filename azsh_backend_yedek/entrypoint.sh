#!/bin/sh
python manage.py collectstatic --noinput
python manage.py migrate --noinput
python manage.py createsuperuser --noinput || true
exec gunicorn --bind 0.0.0.0:8080 \
    --workers 3 \
    --threads 4 \
    --worker-class gthread \
    --timeout 60 \
    azshserver.wsgi:application
