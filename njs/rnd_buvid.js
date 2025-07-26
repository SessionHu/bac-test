// 简单的MD5函数实现（仅用于示例，实际使用时建议使用成熟的库）
function md5(str) {
    // 这里使用了一个简化的MD5实现，实际应用中应使用成熟的库
    const crypto = require('crypto');
    return crypto.createHash('md5').update(str).digest('hex');
}

// buvid 函数实现
function buvid() {
    var mac = [];
    for (let i = 0; i < 6; i++) {
        var min = Math.min(0, 0xff);
        var max = Math.max(0, 0xff);
        var num = parseInt((Math.random() * (min - max + 1) + max).toString()).toString(16);
        mac.push(num);
    }
    var md5Hash = md5(mac.join(':'));
    var md5Arr = md5Hash.split('');
    return `XY${md5Arr[2]}${md5Arr[12]}${md5Arr[22]}${md5Hash}`;
}

// 调用 buvid 函数并输出结果
console.log(buvid().toUpperCase());

