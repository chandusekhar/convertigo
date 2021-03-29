    /**
     * Function RootPageAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    RootPageAction(page: C8oPageBase, props, vars) : Promise<any> {
	    function toString(data) {
	        if (data) {
	            try {
	                return JSON.stringify(data);
	            } catch(e) {
	                return data.toString();
	            }
	        } else {
	           return "no data"; 
	        }
	    }
		
        return new Promise((resolve, reject) => {
            let q:string = props.page; // qname of page
            let p:string = q.substring(q.lastIndexOf('.')+1);
            let version:string = props.tplVersion ? props.tplVersion : '';
            let greater: any = typeof page["compare"]!== "undefined" ? page["compare"]("7.7.0.2", version) : version.localeCompare("7.7.0.2");
            let v:any = greater ? p : page.getPageByName(p);
            
            page.routerProvider.setRoot(v, props.data, {
                animate: props.animate == "true" ? true:false,
                duration: props.animate_duration
            })
            .then((res:any) => {
				page.c8o.log.debug("[MB] Page '"+p+"' rooted with data: " + toString(props.data));
                resolve(res)
            }).catch((error:any) => {
				page.c8o.log.debug("[MB] Could not root to page '"+p+"'");
                reject(error)
            })
        });
    }