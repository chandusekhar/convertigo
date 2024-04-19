// testPlatformStore.js
import { writable } from 'svelte/store';
import { call } from '$lib/utils/service';
export let connectorsStore = writable([]);
export let transactionsStore = writable([]);

export async function getProjectTestPlatform(projectName) {
	try {
		const res = await call('projects.GetTestPlatform', { projectName });
		let connectors = res?.admin?.project?.connector || [];
		if (!Array.isArray(connectors)) {
			connectors = [connectors];
		}
		connectorsStore.set(connectors);
		let transactions = res?.admin?.project?.connector?.transaction || [];
		if (!Array.isArray(transactions)) {
			transactions = [transactions];
		}
		transactionsStore.set(transactions);
	} catch (error) {
		console.error('Error fetching project test platform:', error);
	}
}
