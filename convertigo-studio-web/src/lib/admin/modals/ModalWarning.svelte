<script>
	import { getModalStore } from '@skeletonlabs/skeleton';
	import Card from '../components/Card.svelte';

	const modalStore = getModalStore();
	const { mode } = $modalStore[0].meta ?? {};
</script>

{#if mode == 'Confirm'}
	<Card class="min-w-[30vw]">
		<div class="p-">
			{#if $modalStore[0]}
				<header class="text-2xl font-bold mb-5">{$modalStore[0].title}</header>
				<article class="mb-10">{$modalStore[0].body}</article>
			{/if}
			<div class="w-full flex justify-end">
				<div class="flex flex-wrap gap-2">
					<div class="flex-1">
						<button
							on:click={() => {
								if ($modalStore[0].response) {
									$modalStore[0].response(false);
									modalStore.close();
								}
							}}
							class="cancel-button w-full"
						>
							Cancel
						</button>
					</div>
					<div class="flex-1">
						<button
							type="button"
							on:click={() => {
								if ($modalStore[0].response) {
									$modalStore[0].response(true);
									modalStore.close();
								}
							}}
							class="w-full confirm-button"
						>
							Confirm
						</button>
					</div>
				</div>
			</div>
		</div>
	</Card>
{/if}

{#if mode == 'Error'}
	<Card>
		<div class="p-5">
			{#if $modalStore[0]}
				<header class="text-2xl font-bold mb-5 text-error-600">{$modalStore[0].title}</header>
				<article class="mb-10 font-normal">{$modalStore[0].body}</article>
			{/if}
			<div class="flex flex-wrap">
				<button
					on:click={() => {
						if ($modalStore[0].response) {
							$modalStore[0].response(false);
							modalStore.close();
						}
					}}
					class="cancel-button"
				>
					Ok
				</button>
			</div>
		</div>
	</Card>
{:else if mode == 'Success'}
	<Card>
		<div class="p-5">
			{#if $modalStore[0]}
				<header class="text-2xl font-bold mb-5 text-secondary-600">{$modalStore[0].title}</header>
				<article class="mb-10">{$modalStore[0].body}</article>
			{/if}
			<div class="flex flex-wrap">
				<button
					on:click={() => {
						if ($modalStore[0].response) {
							$modalStore[0].response(false);
							modalStore.close();
						}
					}}
					class="cancel-button"
				>
					Ok
				</button>
			</div>
		</div>
	</Card>
{:else if mode == 'Warnings'}
	<Card>
		<div class="p-5">
			{#if $modalStore[0]}
				<header class="text-2xl font-bold mb-5">{$modalStore[0].title}</header>
				<article class="mb-10">{$modalStore[0].body}</article>
			{/if}
			<div class="flex flex-wrap">
				<button
					on:click={() => {
						if ($modalStore[0].response) {
							$modalStore[0].response(false);
							modalStore.close();
						}
					}}
					class="cancel-button"
				>
					Ok
				</button>
			</div>
		</div>
	</Card>
{/if}
