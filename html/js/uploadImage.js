"use strict";

// 40 MB
const MAX_IMAGE_SIZE = 40000000;


function handleUploadImage() {
    const form = document.uploadImageForm;
    form.uploadButton.disabled = true;

    let data = {};
    // TODO assign a guid name on server instead of using user file_name
    // TODO strip metadata on server side
    // data["img_name"] = document.querySelector('input[type=file]').files[0].name;
    data["lat"] = 0;
    data["lon"] = 0;
    data["timestamp"] = 0;

    // Removes preamble which states this is a base64 value
    const segments = form.base64EncodedValue.value.split(',');
    data["base64encoded"] = segments[1];

    let js = JSON.stringify(data);
    console.log("JS:" + js);
    const xhr = new XMLHttpRequest();
    xhr.open("POST", upload_image_url, true);
    xhr.send(js);

    xhr.onloadend = function() {
        console.log(xhr);
        console.log(xhr.request);
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.statusCode === 200) {
                console.log("XHR:" + xhr.responseText);
                alert("Uploaded Image")
                location.reload();
            } else {
                console.log("actual:" + xhr.responseText);
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
        document.uploadImageForm.base64EncodedValue.value = "";
        alert("File size too large to use: " + files[0].size + " bytes");
    } else {
        // Run Metadata checks here
        const reader = new FileReader();
        reader.readAsDataURL(files[0]);

        reader.onload = function () {
            document.uploadImageForm.base64EncodedValue.value = reader.result;
            document.uploadImageForm.uploadButton.disabled = false;
        }
    }
}