//@ts-check
'use strict';

// check argv
if (process.argv.length < 4) {
    console.log('Usage: node upload_cover.js <file> <cookie>');
    process.exit(1);
}

// cookie
const cookie = process.argv[3];

// data
let data = '';

// csrf
data += 'csrf=';
cookie.split(/;\s*/).forEach(item => {
    if (item.startsWith('bili_jct=')) {
        data += item.replace('bili_jct=', '');
    }
});

// cover
data += '&cover=';

const fs = require('fs');

// get file minetype from the file name
let suffix = (process.argv[2].split('.').pop() || 'jpeg').toLowerCase();
if (suffix === 'jpg') suffix = 'jpeg';
const mimeType = `image/${suffix}`;

// add the prefix of data
const ph = `data:${mimeType};base64,`
data += encodeURIComponent(ph);

// read
data += encodeURIComponent(fs.readFileSync(process.argv[2], { encoding: 'base64' }));

// send
async function send() {
    const response = await fetch('https://member.bilibili.com/x/vu/web/cover/up?ts=' + Date.now(), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Cookie': cookie
        },
        body: data
    });
    const result = await response.json();
    console.log(result);
}

send();
