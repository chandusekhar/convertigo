<script>
	import Card from '$lib/admin/components/Card.svelte';
	import Icon from '@iconify/svelte';
	import { getModalStore } from '@skeletonlabs/skeleton';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import TableAutoCard from '$lib/admin/components/TableAutoCard.svelte';
	import { usersList, usersStore } from '$lib/admin/stores/rolesStore';

	const rolesModalStore = getModalStore();

	onMount(() => {
		usersList();
	});

	async function deleteUsersRoles(username) {
		const formData = new FormData();
		formData.append('username', username);
		try {
			//@ts-ignore
			const res = await call('roles.Delete', formData);
			console.log('service delete roles', res);
			await usersList();
		} catch (error) {
			console.error('Error deleting user role:', error);
		}
	}

	async function deleteAllRoles() {
		try {
			const res = await call('roles.DeleteAll');
			console.log('service delete All roles', res);
			usersList();
		} catch (err) {
			console.error(err);
		}
	}

	function openModals(mode, row) {
		let title = '';

		switch (mode) {
			case 'add':
				title = 'Add User';
				break;
			case 'import':
				title = 'Import Users';
				break;
			case 'export':
				title = 'Export Users';
				break;
			case 'edit':
				title = 'Edit User Role';
				break;
		}

		rolesModalStore.trigger({
			type: 'component',
			component: 'modalRoles',
			meta: { mode, row },
			title: title
		});
	}

	function openDeleteAllModal() {
		rolesModalStore.trigger({
			type: 'component',
			component: 'modalWarning',
			title: 'You are going to delete All Roles',
			body: 'Are you sure you want to ?',
			meta: { mode: 'Confirm' },
			response: (confirmed) => {
				if (confirmed) {
					deleteAllRoles();
				}
			}
		});
	}

	function openDeleteModal(userName) {
		rolesModalStore.trigger({
			type: 'component',
			component: 'modalWarning',
			title: 'Please Confirm',
			body: 'Are you sure you want to delete the role ?',
			meta: { mode: 'Confirm' },
			response: (confirmed) => {
				if (confirmed) {
					deleteUsersRoles(userName);
				}
			}
		});
	}

	function getTailwindClass(role) {
		role = role.toUpperCase();
		if (role.endsWith('VIEW')) {
			return 'bg-green-500 mt-2 mr-5 ml-5';
		} else if (role.endsWith('CONFIG')) {
			return 'bg-blue-500 mt-2 mr-5 ml-5';
		} else {
			return 'bg-yellow-500 mt-2 mr-5 ml-5';
		}
	}
</script>

<Card title="Roles">
	<div slot="cornerOption">
		<button class="w-full bg-error-400-500-token" on:click={openDeleteAllModal}>
			<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7 mr-3" />
			Delete All Roles
		</button>
	</div>
	<div class="flex flex-wrap gap-2 mb-10 mt-10">
		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token" on:click={() => openModals('add')}>
				<Icon icon="material-symbols-light:add" class="w-7 h-7 mr-3" />
				Add User
			</button>
		</div>
		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token" on:click={() => openModals('import')}>
				<Icon icon="solar:import-line-duotone" class="w-7 h-7 mr-3" />
				Import Users
			</button>
		</div>
		<div class="flex-1">
			<button class="w-full bg-primary-400-500-token" on:click={() => openModals('export')}>
				<Icon icon="solar:export-line-duotone" class="w-7 h-7 mr-3" />
				Export Users
			</button>
		</div>
	</div>

	{#if $usersStore.length >= 0}
		<TableAutoCard
			definition={[
				{ name: 'Name', key: 'name' },
				{ name: 'Role', key: 'role', custom: true },
				{ name: 'Edit', custom: true },
				{ name: 'Delete', custom: true }
			]}
			data={$usersStore}
			let:row
			let:def
		>
			{#if def.name === 'Role'}
				{#each row.role as role}
					{role.replace(/_/g, ' ')}
				{/each}
			{:else if def.name === 'Edit'}
				<button
					class="p-1 px-2 shadow-md bg-tertiary-400-500-token"
					on:click={() => openModals('add', row)}
				>
					<Icon icon="bitcoin-icons:edit-outline" class="w-7 h-7" />
				</button>
			{:else if def.name === 'Delete'}
				<button
					class="p-1 px-2 shadow-md bg-error-400-500-token"
					on:click={() => openDeleteModal(row.name)}
				>
					<Icon icon="material-symbols-light:delete-outline" class="w-7 h-7" />
				</button>
			{/if}
		</TableAutoCard>
	{:else}
		<div class="table-container">
			<table class="rounded-token table">
				<thead class="rounded-token">
					<tr>
						{#each Array(5) as _}
							<th class="header dark:bg-surface-800">
								<div class="my-2 h-8 placeholder animate-pulse"></div>
							</th>
						{/each}
					</tr>
				</thead>
				<tbody>
					{#each Array(5) as _}
						<tr>
							{#each Array(5) as _}
								<td>
									<div class="my-2 h-8 placeholder animate-pulse"></div>
								</td>
							{/each}
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}
</Card>
