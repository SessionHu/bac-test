import crypto from 'crypto';

function app_sign(device_info: Uint8Array, x_exbadbasket: Uint8Array, x_fingerprint: Uint8Array): string {
    const hmac = crypto.createHmac('sha256', 'Ezlc3tgtl');
    hmac.update(device_info);
    hmac.update('x-exbadbasket').update(x_exbadbasket);
    hmac.update('x-fingerprint').update(x_fingerprint);
    return hmac.digest('hex');
}

function web_sign(timestamp: number): string {
    const hmac = crypto.createHmac('sha256', 'XgwSnGZ1p');
    hmac.update(`ts${timestamp}`);
    return hmac.digest('hex');
}

async function gen_bili_ticket(mark: 'ec01' | 'ec02', hexsign: string, timestamp: number, csrf: string | null): Promise<any> {
    const url: string = 'https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket';
    const params: URLSearchParams = new URLSearchParams({
        key_id: mark,
        hexsign: hexsign,
        'context[ts]': timestamp.toString(),
        csrf: csrf || ''
    });
    try {
        const response = await fetch(`${url}?${params.toString()}`, {
            method: 'POST',
            headers: {
                'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0'
            }
        });
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        return data;
    } catch (e) {
        throw e;
    }
}

(async () => {
    try {
        const timestamp: number = Math.floor(Date.now() / 1000);
        const apphexsign: string = app_sign(
            new Uint8Array([0x1, 0x2, 0x3, 0x4]),
            new Uint8Array([0x5, 0x6, 0x7, 0x8]),
            new Uint8Array([0x5, 0x6, 0x7, 0x8]),
        );
        const webhexsign: string = web_sign(timestamp);
        console.log('AppHexSign:', apphexsign);
        console.log('WebHexSign:', webhexsign);
        const appbiliticket = await gen_bili_ticket('ec01', apphexsign, timestamp, null);
        const webbiliticket = await gen_bili_ticket('ec02', webhexsign, timestamp, null);
        console.log('AppBiliTicket:', appbiliticket);
        console.log('WebBiliTicket:', webbiliticket);
    } catch (e) {
        console.error('Failed to get BiliTicket:', e);
    }
})();
