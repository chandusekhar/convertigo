<script>
	import { selectedId } from '$lib/treeview/treeStore';
	import { getUrl } from '$lib/utils/service';
	import Icon from '@iconify/svelte';
	import { AppBar, AppRail, AppRailAnchor, AppShell, ProgressRadial } from '@skeletonlabs/skeleton';
	import { afterUpdate, onMount } from 'svelte';

	let project = '';
	let message = '';
	/** @type {WebSocket|null} */
	let ws = null;

	/** @type {HTMLIFrameElement} */
	let iframe;

	let iframeUrl = null;
	let progress = 0;
	let output = '';

	$: project = $selectedId.replace(/(.*?)[\.:].*/, '$1');

	$: {
		if (project != '' && ws?.readyState == WebSocket.OPEN) {
			message = '';
			runAction('attach');
		}
	}

	onMount(() => {
		ws = new WebSocket(
			`${getUrl().replace(
				/^http/,
				'ws'
			)}studio.ngxbuilder.WsBuilder?__xsrfToken=${localStorage.getItem('x-xsrf-token')}`
		);
		ws.onopen = () => {
			ws = ws;
		};
		ws.onmessage = (msg) => {
			try {
				const { type, value } = JSON.parse(msg.data);
				switch (type) {
					case 'output':
						output = value;
					case 'log':
						message = message + `${value}\n`;
						break;
					case 'progress':
						progress = Number.parseFloat(value);
						break;
					case 'load':
						iframeUrl = value;
						break;
				}
			} catch (e) {
				message = message + `Exception: ${e}\n`;
			}
		};
		return () => {
			ws?.close();
			ws = null;
		};
	});

	let textarea;
	let startY, startHeight;

	/** @param {MouseEvent} e*/
	function onDrag(e) {
		startY = e.clientY;
		startHeight = parseInt(document.defaultView?.getComputedStyle(textarea)?.height ?? '', 10);
		document.documentElement.addEventListener('mousemove', doDrag, false);
		document.documentElement.addEventListener('mouseup', stopDrag, false);
	}

	/** @param {MouseEvent} e*/
	function doDrag(e) {
		textarea.style.height = startHeight - (e.clientY - startY) + 'px';
	}

	function stopDrag() {
		document.documentElement.removeEventListener('mousemove', doDrag, false);
		document.documentElement.removeEventListener('mouseup', stopDrag, false);
	}

	afterUpdate(() => {
		if (textarea) {
			textarea.scrollTop = textarea.scrollHeight;
		}
	});

	function runAction(action) {
		progress = 0;
		iframeUrl = null;
		output = 'Not running';
		ws?.send(
			JSON.stringify({
				action,
				project
			})
		);
	}
</script>

{#if project == ''}
	<h1>No project selected!</h1>
{:else if message == ''}
	<h1>Checking for project <u>{project}</u>!</h1>
{/if}
{#if project != '' && message != ''}
	<!-- svelte-ignore a11y-no-static-element-interactions -->
	<AppShell>
		<svelte:fragment slot="header">
			<AppBar padding="p-1 pl-4" background="dark:bg-surface-500 bg-surface-50">
				<svelte:fragment slot="lead">
					<p>NGX Builder for {project}</p>
				</svelte:fragment>

				<svelte:fragment slot="header"></svelte:fragment>

				<svelte:fragment slot="trail">
					<button
						on:click={() => runAction('build_dev')}
						type="button"
						class="p-2 rounded-xl bg-surface-700">Run</button
					>
					<button
						on:click={() => runAction('kill')}
						type="button"
						class="p-2 rounded-xl bg-surface-700">Kill</button
					>
				</svelte:fragment>
			</AppBar>
		</svelte:fragment>

		<!--
		<svelte:fragment slot="sidebarLeft">
			<AppRail width="auto" class="p-2" background="bg-surface-900" >

				<AppRailAnchor on:click={() => runAction('build_dev')}>
					<button type="button" class="p-3 rounded-xl bg-surface-700">Run</button>
				</AppRailAnchor>
				<AppRailAnchor on:click={() => runAction('kill')}>
					<button type="button" class="p-3 rounded-xl bg-surface-700">Kill</button>
				</AppRailAnchor>
			</AppRail>
		</svelte:fragment> -->
		{#if iframeUrl == null}
			<div class="flex flex-col justify-around items-center h-full w-full">
				<ProgressRadial value={progress}>{progress}%</ProgressRadial>
				<div class="card variant-ghost w-3/4 py-1 text-center">{output}</div>
			</div>
		{:else}
			<iframe bind:this={iframe} class="h-full w-full" title="test" src={iframeUrl} />
		{/if}
		<svelte:fragment slot="footer">
			<div class="draggable border-4 w-full" on:mousedown={onDrag} />
			<textarea
				bind:this={textarea}
				readonly={true}
				class="bg-white dark:bg-gray-100 h-full w-full resize-none">{message}</textarea
			>
		</svelte:fragment>
	</AppShell>
{/if}

<style lang="postcss">
	.draggable {
		cursor: grab;
	}
</style>
