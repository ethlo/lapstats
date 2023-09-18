<!DOCTYPE html>
<html lang="en">
<head>
<title>Race overview</title>
<style>

html, body {
margin: 0
padding:0
height:100%
width:100;
}

html, body, td {
font-family: verdana, arial, sans-serif;
font-size: 12px;
}

div.content {
    margin:5%
}

td, th {
    border: 1px solid #777;
    padding: 0.5rem;
    text-align: center;
}

.implicit {
    color: #999;
}

.maxtime {
    color: red;
    font-weight: bold;}

.mintime {
    color: #00aa00;
    font-weight: bold;
}

table {
    width:100%;
}

thead tr {
    background: #1e415a;
    color: #ddd;
}

table {
    border-collapse: collapse;
}

tbody tr:nth-child(odd) {
    background: #eee;
}
caption {
    font-size: 0.8rem;
}
</style>
</head>
<body>
<div class="content">
<h2>Driver timing</h2>
<table>
<thead>
    <tr>
        <th>Driver</th>
        {% for driver in data.drivers %}<td>{{ driver.name }}</td>{% endfor %}
    </tr>
</thead>
<tbody>
<tr>
    <th>Best time</th>
        {% for l in data.minLapTimes %}
             <td>{{ l.value.toMillis() | date('mm:ss.SSS') }}</td>
        {% endfor %}
</tr>
<tr>
    <th>Average time</th>
        {% for l in data.averageLapTimes %}
             <td>{{ l.value.toMillis() | date('mm:ss.SSS') }}</td>
        {% endfor %}
</tr>
<tr>
    <th>Worst time</th>
        {% for l in data.maxLapTimes %}
             <td>{{ l.value.toMillis() | date('mm:ss.SSS') }}</td>
        {% endfor %}
</tr>
</tbody>
</table>

<h2>Laps</h2>
<table>
<thead>
    <tr>
        <td>Lap</td>
        {% for driver in data.drivers %}<td>{{ driver.name }}</td>{% endfor %}
    </tr>
</thead>
<tbody>
        {% for entry in data.laps %}
        <tr><td>{{ entry.key }}</td>
        {% for l in entry.value %}
        <td class="{{ l.implicit ? 'implicit' : '' }}">
            {{ l.accumulatedLapTime.toMillis() | date('mm:ss.SSS') }} {% if l.implicit %}*{% endif %}
            <span class="{{ l.minLapTime == l.timing.time ? 'mintime' : '' }} {{ l.maxLapTime == l.timing.time ? 'maxtime' : '' }}">({{ l.timing.time.toMillis()  | date('mm:ss.SSS') }})</span></td>
        {% endfor %}
        </tr>
        {% endfor %}
    </tbody>
</table>
</div>
</body>
</html>
