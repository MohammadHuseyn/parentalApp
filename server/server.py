import json
import os
import re
import string
import base64
from flask import Flask, jsonify, request
from PIL import Image
import pytesseract 


# this sever code uses flask
app = Flask(__name__)

# tesseract directory
# CHANGE THIS!
pytesseract.pytesseract.tesseract_cmd = r'C://Program Files//Tesseract-OCR//tesseract.exe'


# in the '/child' endpoint
# requests that come from ThirdTry
@app.route('/child', methods=['POST'])
def child():
    # get the request
    json_data = request.get_json()
    if "images" in json_data:

        # store all images comming from child to server
        for key, value in json_data["images"].items():
            # decode images with base64
            img = base64.b64decode(value)

            # some chars are not supported -> remove them
            key = re.sub(r'[^\w\-_. ]', '', key)
            # save the file in images/
            with open('images/' + key, "wb") as fh:
                fh.write(img)

    # create a response
    res = {"ack": True}
    # fill the response with the badwords
    with open('badwords.txt', 'r') as file:
        data = file.read()
        res["words"] = data
    # return the response
    return jsonify(res)



# in the '/parent' endpoint
# requests that come from ParentTry
@app.route('/parent', methods=['POST'])
def parent():

    # get the request
    json = request.get_json()
    # open the badwords file and put the words that the parent wants
    with open("badwords.txt", "w") as text_file:
        text_file.write(json["words"])
    # create a response var
    res = {"images" : {}}
    # if the parent wants the images...
    if json["images"] == True:
        # for in the images that are store in the images/
        directory = os.fsencode("C://Users//Mohammad Hosein//StudioProjects//server//images")
        for file in os.listdir(directory):
            filename = os.fsdecode(file)
            if filename.endswith(".jpg") or filename.endswith(".png"): 
                with open('images/' + filename, mode='rb') as file:
                    img = file.read()

                    # encode each image with base64 and put the in res
                    res["images"][filename] = base64.encodebytes(img).decode('utf-8')
    # return the response
    return jsonify(res)

# in the '/online' endpoint
# requests that come from ThirdTry for server availablity
@app.route('/online', methods=['GET', 'POST'])
def online ():
    json = request.get_json()
    # I AM ONLINE!
    return jsonify({"online" : True})


# run flask app
if __name__ == '__main__':
   app.run(port=8000)