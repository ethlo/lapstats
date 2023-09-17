Lap,{% for driver in data.drivers %}{{ driver.name }},{% endfor %}
{% for entry in data.laps %}

{{ entry.key }},{% for l in entry.value %}{{ l.accumulatedLapTime.toMillis() | date('mm:ss.SSS') }},{% endfor %}
{% endfor %}