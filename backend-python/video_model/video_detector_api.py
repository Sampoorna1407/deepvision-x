from flask import Flask, request, jsonify

import tensorflow as tf

import cv2

import numpy as np

print("Loading Video Model...")

# LOAD MODEL
model = tf.keras.models.load_model(
    'ai_video_detector.h5',
    compile=False
)

print("Video Model Loaded!")

app = Flask(__name__)

IMG_SIZE = 64

@app.route('/detect-video', methods=['POST'])
def detect_video():

    try:

        file = request.files['file']

        path = 'temp.mp4'

        file.save(path)

        cap = cv2.VideoCapture(path)

        predictions = []

        while True:

            ret, frame = cap.read()

            if not ret:
                break

            frame = cv2.resize(
                frame,
                (IMG_SIZE, IMG_SIZE)
            )

            frame = frame / 255.0

            frame = np.expand_dims(
                frame,
                axis=0
            )

            pred = model.predict(frame)[0][0]

            predictions.append(pred)

        cap.release()

        avg_pred = np.mean(predictions)

        if avg_pred > 0.5:

            result = "Deepfake Video"

        else:

            result = "Real Video"

        return jsonify({
            "result": result
        })

    except Exception as e:

        return jsonify({
            "error": str(e)
        })

if __name__ == '__main__':

    app.run(
        host='0.0.0.0',
        port=5003,
        debug=True
    )