import { XMLParser } from 'fast-xml-parser';
import { loading } from '$lib/utils/loadingStore';

let cpt = 0;
loading.subscribe((n) => (cpt = n));
/**
 * @param {string} service
 * @param {string | Record<string, string> | string[][] | URLSearchParams | undefined} data
 */
export async function call(service, data = {}) {
	let url = getUrl() + service;
	loading.set(cpt + 1);
	let res = await fetch(url, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded',
			'x-xsrf-token': localStorage.getItem('x-xsrf-token') ?? 'Fetch'
		},
		body: new URLSearchParams(data),
		credentials: 'include'
	});
	loading.set(cpt - 1);
	var xsrf = res.headers.get('x-xsrf-token');
	if (xsrf != null) {
		localStorage.setItem('x-xsrf-token', xsrf);
	}

	const contentType = res.headers.get('content-type');
	if (contentType?.includes('xml')) {
		return new XMLParser({ ignoreAttributes: false }).parse(await res.text());
	} else {
		return await res.json();
	}
}

export async function callXml(service, xmlPayload) {
	let url = getUrl() + service;
	loading.set(cpt + 1);

	let headers = {
		'Content-Type': 'application/xml',
		'x-xsrf-token': localStorage.getItem('x-xsrf-token') ?? 'Fetch'
	};

	let res = await fetch(url, {
		method: 'POST',
		headers: headers,
		body: xmlPayload,
		credentials: 'include'
	});

	loading.set(cpt - 1);
	var xsrf = res.headers.get('x-xsrf-token');
	if (xsrf != null) {
		localStorage.setItem('x-xsrf-token', xsrf);
	}

	const contentType = res.headers.get('content-type');
	if (contentType?.includes('xml')) {
		return new XMLParser({ ignoreAttributes: false }).parse(await res.text());
	} else {
		return await res.json();
	}
}


export async function callFormUrlEncoded(service, params) {
    let url = getUrl() + service;
    loading.set(cpt + 1);

    let encodedParams = Object.keys(params).map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`).join('&');

    let headers = {
        'Accept': 'application/xml, text/xml, */*; q=0.01', 
        'Content-Type': 'application/x-www-form-urlencoded',
        'x-xsrf-token': localStorage.getItem('x-xsrf-token') ?? 'Fetch'
    };

    let res = await fetch(url, {
        method: 'POST',
        headers: headers,
        body: encodedParams,
        credentials: 'include'
    });

    loading.set(cpt - 1);
    var xsrf = res.headers.get('x-xsrf-token');
    if (xsrf != null) {
        localStorage.setItem('x-xsrf-token', xsrf);
    }

    const contentType = res.headers.get('content-type');
    if (contentType?.includes('xml')) {
        return new XMLParser({ ignoreAttributes: false }).parse(await res.text());
    } else {
        return await res.json();
    }
}


export function getUrl() {
	const m = window.location.pathname.match('^(.*?)/studio/');
	return `${window.location.origin}${m ? m[1] : '/convertigo'}/admin/services/`;
}

// $lib/utils/xmlConverter.js

export function toXml(data) {
	let xml = '<?xml version="1.0" encoding="UTF-8"?>';
	xml += '<configurations>';
	xml += '</configurations>';
	return xml;
}

/* studio.dbo service methods */

/**
 * @param {string} target - the id of the parent dbo in tree
 * @param {string} position - the position relative to target inside|first|after
 * @param {any} data - the json data object
 */
export async function addDbo(
	target = '',
	position = 'inside',
	data = { kind: '', data: { id: '' } }
) {
	let result = await call('studio.dbo.Add', { target, position, data: JSON.stringify(data) });
	return result;
}

/**
 * @param {string} target - the id of the parent dbo in tree
 * @param {string} position - the position relative to target inside|first|after
 * @param {any} data - the json data object
 */
export async function moveDbo(
	target = '',
	position = 'inside',
	data = { kind: '', data: { id: '' } }
) {
	let result = await call('studio.dbo.Move', { target, position, data: JSON.stringify(data) });
	return result;
}

/**
 * @param {string} action - the drag action (move|copy)
 * @param {string} target - the id of the parent dbo in tree
 * @param {string} position - the position relative to target inside|first|after
 * @param {any} data - the json data object
 */
export async function acceptDbo(
	action = 'move',
	target = '',
	position = 'inside',
	data = { kind: '', data: { id: '' } }
) {
	let result = await call('studio.dbo.Accept', {
		action,
		target,
		position,
		data: JSON.stringify(data)
	});
	return result;
}

/**
 * @param {string} id - the id of the dbo in tree
 */
export async function removeDbo(id = '') {
	let result = await call('studio.dbo.Remove', { id });
	return result;
}

/**
 * @param {any} ids - the array of tree dbo ids
 */
export async function cutDbo(ids = []) {
	let result = await call('studio.dbo.Cut', { ids });
	return result;
}

/**
 * @param {any} ids - the array of tree dbo ids
 */
export async function copyDbo(ids = []) {
	let result = await call('studio.dbo.Copy', { ids });
	return result;
}

/**
 * @param {string} target - the id of the target dbo in tree
 * @param {string} xml - the xml string
 */
export async function pasteDbo(target = '', xml = '') {
	let result = await call('studio.dbo.Paste', { target, xml });
	return result;
}

/**
 * @param {string} id - the id of the target dbo in tree
 * @param {string} name - the dbo new name
 * @param {string} update - UPDATE_ALL | UPDATE_LOCAL | UPDATE_NONE
 */
export async function renameDbo(id = '', name = '', update = 'UPDATE_NONE') {
	let result = await call('studio.dbo.Rename', { id, name, update });
	return result;
}
