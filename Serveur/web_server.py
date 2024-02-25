from flask import Flask, request, jsonify, render_template
import json
app = Flask(__name__)

mydata = []
headings = ("étudiant", "heure d'arrivée")


seen_students = set()

@app.route("/")
def table():
    return render_template("index.html", headings=headings, mydata=mydata)

@app.route("/android_ml", methods=['POST'])
def print_json():
    try:
        data_list = request.get_json(force=True)
    except Exception as e:
        return jsonify(error=str(e)), 400

    for data in data_list:
        nom = data.get('name')
        heure = data.get('time')
        
        # Si nous n'avons pas encore vu cet étudiant, ajoutez-le à mydata et à seen_students
        if nom not in seen_students:
            seen_students.add(nom)
            mydata.append({'nom_etudiant': nom, 'heure arrivé': heure})
            print(mydata)
    
    return render_template("index.html", headings=headings, mydata=mydata)

if __name__ == "__main__":
    app.run(host="0.0.0.0", debug=True)