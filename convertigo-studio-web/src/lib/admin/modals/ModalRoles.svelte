<script>
	import { RadioGroup, RadioItem, getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';
	import { call } from '$lib/utils/service';
	import { onMount } from 'svelte';
	import Icon from '@iconify/svelte';
	import ResponsiveContainer from '../components/ResponsiveContainer.svelte';
	import { usersList, rolesStore } from '../stores/rolesStore';
	import CheckState from '../components/CheckState.svelte';

	const modalStore = getModalStore();
	const { mode, row } = $modalStore[0].meta ?? {};

	export let parent;

	let importAction = '';
	let importPriority = 'priority-import';

	onMount(() => {
		if (mode == 'add' && row?.role) {
			for (let part of $rolesStore) {
				for (let role of part.roles) {
					role.checked = row.role.includes(role.value);
				}
				part.toggle = part.roles.findIndex((role) => !role.checked) == -1;
			}
		}
	});

	function toggleRoles(checked, roles) {
		roles.forEach((role) => (role.checked = checked));
	}

	async function rolesAdd(event) {
		event.preventDefault();
		const fd = new FormData(event.target);

		//@ts-ignore
		const res = await call(`roles.${row ? 'Edit' : 'Add'}`, fd);
		console.log('role add res:', res);
		if (res?.admin?.response?.['@_state'] == 'success') {
			usersList();
			modalStore.close();
		}
	}

	/**
	 * @param {Event} e
	 */

	async function importRoles(e) {
		//@ts-ignore
		const fd = new FormData(e.target.form);
		try {
			//@ts-ignore
			const response = await call('roles.Import', fd);
			console.log(response);
		} catch (err) {
			console.error(err);
		}
	}
</script>

{#if mode == 'add'}
	<Card title={`${row ? 'Edit' : 'Add'} User`} style="padding: 40px;">
		<form on:submit={rolesAdd}>
			{#if row}
				<input type="hidden" name="oldUsername" value={row.name} />
			{/if}
			<div class="flex items-center gap-10 mb-10">
				<label class="border-common">
					<p class="label-common text-input">Name</p>
					<input
						class="input-common"
						type="text"
						name="username"
						placeholder="Enter name …"
						value={row?.name ?? ''}
					/>
				</label>

				<label class="border-common">
					<p class="label-common">Password</p>
					<input
						class="input-common"
						type="password"
						name="password"
						placeholder={row ? 'Leave blank for no change…' : 'Enter password …'}
						value=""
					/>
				</label>
			</div>

			<ResponsiveContainer
				scrollable={true}
				smCols="sm:grid-cols-1"
				mdCols="md:grid-cols-3"
				lgCols="lg:grid-cols-3"
			>
				{#each $rolesStore as { name, end, roles, toggle }}
					<div class="container-child">
						<div class="flex items-center gap-5">
							<h1 class="font-bold text-xl">{name}</h1>
							<RadioGroup class="shadow-md">
								{@const radioDef = [
									{ value: false, active: 'variant-filled-surface', icon: 'mdi:minus' },
									{ value: true, active: 'variant-filled-secondary', icon: 'mdi:plus' }
								]}
								{#each radioDef as { value, active, icon }}
									<RadioItem
										bind:group={toggle}
										on:click={() => toggleRoles(value, roles)}
										name="viewRoles"
										{value}
										{active}
									>
										<Icon {icon} class="w-6 h-6" />
									</RadioItem>
								{/each}
							</RadioGroup>
						</div>
						<div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-2 gap-5 mt-10">
							{#each roles as { value, checked, description }}
								<div class="flex items-center" title={description}>
									<CheckState name="roles" {value} {checked}>
										{value.replace(end, '').replace(/_/g, ' ')}
									</CheckState>
								</div>
							{/each}
						</div>
					</div>
				{/each}
			</ResponsiveContainer>

			<div class="flex gap-5 mt-10">
				<button
					class="bg-surface-400-500-token w-60"
					on:click|preventDefault={() => modalStore.close()}>Cancel</button
				>
				<button type="submit" class="confirm-button w-60">Confirm</button>
			</div>
		</form>
	</Card>
{/if}

{#if mode == 'import'}
	<Card>
		<form class="p-5 rounded-xl glass flex flex-col">
			<h1 class="text-xl mb-5 text-center">Import users</h1>
			<RadioGroup active="bg-secondary-400-500-token">
				<RadioItem bind:group={importAction} name="action-import" value="clear-import"
					>Clear & import</RadioItem
				>
				<RadioItem bind:group={importAction} name="action-import" value="">Merge users</RadioItem>
			</RadioGroup>
			{#if importAction == ''}
				<p class="mt-10 text-[14px] mb-5 text-center">In case of name conflict</p>
				<RadioGroup active="bg-secondary-400-500-token">
					<RadioItem bind:group={importPriority} name="priority" value="priority-server"
						>Priority Server</RadioItem
					>
					<RadioItem bind:group={importPriority} name="priority" value="priority-import"
						>Priority import</RadioItem
					>
				</RadioGroup>
			{/if}
			<p class="font-medium mt-10">Actual users list will be saved aside in a backup file.</p>

			<div class="flex flex-wrap gap-5">
				<div class="flex-1">
					<button class="mt-5 w-full cancel-button" on:click={() => modalStore.close()}
						>Cancel</button
					>
				</div>
				<div class="flex-1">
					<input
						type="file"
						name="userfile"
						id="symbolUploadFile"
						accept=".properties"
						class="hidden"
						on:change={importRoles}
					/>
					<label for="symbolUploadFile" class="confirm-button btn mt-5 w-full">Import</label>
				</div>
			</div>
		</form>
	</Card>
{/if}

{#if mode == 'export'}
	export user
{/if}

{#if mode == 'delete all'}
	Delete all
{/if}

<style lang="postcss">
	.container-child {
		@apply flex flex-wrap flex-col;
	}
</style>
