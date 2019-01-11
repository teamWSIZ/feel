# Feel app

App zbierający głosy "1" lub "-1" na jedną z podanych pokojów (room).

Wyniki są eksponowane do `prometheus`-a.

### API

http://localhost:8050/status    #status

http://localhost:8050/rooms     #dostępne pokoje

http://localhost:8050/vote/salaE/1 #vote `1` or `-1` for the room `salaE`


### Presentation

* `prometheus.yml`: (in `/resources`)

* run: `./prometheus --config.file=prometheus.yml`

* prometheus GUI: http://localhost:9090/graph

* grafana: http://localhost:3000/

