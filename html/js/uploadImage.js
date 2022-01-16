"use strict";

// 40 MB
const MAX_IMAGE_SIZE = 40000000;


function handleUploadImage(email, lat, lon, timestamp) {
    const form = document.uploadImageForm;
    document.getElementById("uploadButton").disabled = true;

    let data = {};
    // TODO extract email of uploader
    data["creator"] = email;
    data["name"] = document.querySelector('input[type=file]').files[0].name;
    data["lat"] = lat;
    data["lon"] = lon;
    data["timestamp"] = timestamp;

    // Removes preamble which states this is a base64 value
    const segments = document.getElementById("base64Encoding").value.split(',');
    data["base64encoded"] = segments[1];

    let js = JSON.stringify(data);
    console.log("JS:" + js);
    const xhr = new XMLHttpRequest();
    xhr.open("POST", 'https://us-east4-still-tensor-338300.cloudfunctions.net/uploadImage', true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send(js);

    xhr.onloadend = function() {
        //console.log(xhr);
        //console.log(xhr.request);
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.statusCode === 200) {
                //console.log("XHR:" + xhr.responseText);
                alert("Uploaded Image")
                location.reload();
            } else {
                //console.log("actual:" + xhr.responseText);
                const js = JSON.parse(xhr.responseText);
                const err = js["response"];
                alert(err);
            }
        }
    };
}


function handleFileSelect(evt) {
    const files = evt.target.files;
    if (files[0].size > MAX_IMAGE_SIZE) {
        document.getElementById("base64Encoding").value = "";
        alert("File size too large to use: " + files[0].size + " bytes");
    } else {
        // TODO Run Metadata checks here
        const reader = new FileReader();
        reader.readAsDataURL(files[0]);

        reader.onload = function () {
            document.getElementById("base64Encoding").value = reader.result;
            document.getElementById("uploadButton").disabled = false;
        }
    }
}