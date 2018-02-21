    /**
     * Function PopoverAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    PopoverAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let q:string = props.page; // qname of page
            let p:string = q.substring(q.lastIndexOf('.')+1);
            let data = props.data ? props.data: {} 
        
            let Popover = page.getInstance(PopoverController)
            Popover.create(page.getPageByName(p), data, {
                showBackdrop            : props.showBackdrop,
                enableBackdropDismiss   : props.enableBackdropDismiss,
                cssClass                : props.cssClass
            }).present({ev: props.event}).then((data) => {
                page.c8o.log.debug("[MB] Popover Page displayed: " + JSON.stringify(data));
                resolve();
            })
        });
    }